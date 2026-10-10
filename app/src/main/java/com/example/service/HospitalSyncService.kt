package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.cloud.CloudDatabaseManager
import com.example.data.AppDatabase
import com.example.data.HospitalRepository
import com.example.fcm.FcmManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class HospitalSyncService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        FcmManager.createNotificationChannel(applicationContext)

        // Hold a partial wake lock so the CPU remains active for real-time Cloud Firestore & push alerts in background
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MBHospital::SyncWakeLock").apply {
                setReferenceCounted(false)
                acquire(24 * 60 * 60 * 1000L) // Safe 24-hour timeout
            }
        } catch (_: Exception) {}
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Attach persistent real-time Cloud Firestore snapshot listeners & pull initial data
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = HospitalRepository(database.hospitalDao())
        CloudDatabaseManager.startRealtimeCloudSync(applicationContext, repository)
        CloudDatabaseManager.reconcileWithCloudServer(applicationContext, repository)

        // Periodic background pulse every 3 minutes to keep listeners active and auto-reconcile
        serviceScope.launch {
            while (isActive) {
                delay(180_000L) // 3 minutes
                try {
                    CloudDatabaseManager.ensureSyncActive(applicationContext, repository)
                    CloudDatabaseManager.reconcileWithCloudServer(applicationContext, repository)
                } catch (_: Exception) {}
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (_: Exception) {}
    }

    private fun createForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, FcmManager.CHANNEL_ID_SYNC)
            .setContentTitle("MB Nursing Home · Cloud Active")
            .setContentText("Real-time clinical sync & background emergency alerts running")
            .setSmallIcon(R.drawable.ic_stat_hospital)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 9001

        fun start(context: Context) {
            try {
                val intent = Intent(context, HospitalSyncService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) {}
        }
    }
}
