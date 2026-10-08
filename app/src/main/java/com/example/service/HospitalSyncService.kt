package com.example.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.cloud.CloudDatabaseManager
import com.example.data.AppDatabase
import com.example.data.HospitalRepository
import com.example.fcm.FcmManager

class HospitalSyncService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        FcmManager.createNotificationChannel(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        // Attach persistent real-time Cloud Firestore snapshot listeners
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = HospitalRepository(database.hospitalDao())
        CloudDatabaseManager.startRealtimeCloudSync(applicationContext, repository)

        return START_STICKY
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
            .setSmallIcon(R.mipmap.ic_launcher)
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
