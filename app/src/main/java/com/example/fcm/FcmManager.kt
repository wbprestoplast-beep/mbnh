package com.example.fcm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.NotificationEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

object FcmManager {
    private const val TAG = "FcmManager"
    const val CHANNEL_ID_HOSPITAL = "hospital_alerts"
    const val CHANNEL_ID_CRITICAL = "hospital_critical_alerts"
    const val CHANNEL_ID_SYNC = "hospital_background_sync"
    const val DEFAULT_TOPIC_BROADCAST = "hospital_broadcasts"
    const val DEFAULT_TOPIC_EMERGENCY = "hospital_emergencies"

    // Persistent unique runtime ID for this phone instance across process lifecycles
    @Volatile
    private var persistentDeviceId: String? = null

    fun getDeviceId(context: Context): String {
        persistentDeviceId?.let { return it }
        val prefs = context.getSharedPreferences("hms_device_identity", Context.MODE_PRIVATE)
        var id = prefs.getString("device_id", null)
        if (id.isNullOrBlank()) {
            id = "dev_" + UUID.randomUUID().toString().take(8)
            prefs.edit().putString("device_id", id).apply()
        }
        persistentDeviceId = id
        return id
    }

    val DEVICE_INSTANCE_ID: String
        get() = persistentDeviceId ?: "dev_default"

    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    private val _subscribedTopics = MutableStateFlow<Set<String>>(
        setOf(DEFAULT_TOPIC_BROADCAST, DEFAULT_TOPIC_EMERGENCY)
    )
    val subscribedTopics: StateFlow<Set<String>> = _subscribedTopics.asStateFlow()

    fun updateToken(token: String) {
        _fcmToken.value = token
        Log.d(TAG, "FCM token updated: $token")
    }

    fun initialize(context: Context) {
        getDeviceId(context)
        createNotificationChannel(context)

        // Provide immediate device endpoint token for Cloud Firestore multi-device message routing
        if (_fcmToken.value == null) {
            _fcmToken.value = "fcm_cloud_inst_${getDeviceId(context)}"
        }

        Log.d(TAG, "Notification channel and real-time cloud dispatch initialized for device ${getDeviceId(context)}")
    }

    fun subscribeToTopic(topic: String) {
        // Record topic subscription for in-app routing and Firestore listener filtering
        _subscribedTopics.value = _subscribedTopics.value + topic
        Log.d(TAG, "Subscribed to hospital alert topic: $topic")
    }

    fun unsubscribeFromTopic(topic: String) {
        _subscribedTopics.value = _subscribedTopics.value - topic
        Log.d(TAG, "Unsubscribed from hospital alert topic: $topic")
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val defaultSoundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .build()

            // General Hospital Alerts Channel
            val generalChannel = NotificationChannel(
                CHANNEL_ID_HOSPITAL,
                "Hospital Alerts & Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for patient updates, routine tasks, and staff announcements."
                enableLights(true)
                lightColor = android.graphics.Color.CYAN
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
                setSound(defaultSoundUri, audioAttributes)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(generalChannel)

            // Critical Emergency & Broadcast Channel
            val criticalChannel = NotificationChannel(
                CHANNEL_ID_CRITICAL,
                "🚨 Emergency & Critical Broadcasts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical emergency codes, ICU vital alerts, and hospital-wide urgent broadcasts."
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
                setShowBadge(true)
                setSound(defaultSoundUri, audioAttributes)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(criticalChannel)

            // Silent Persistent Background Service Channel
            val syncChannel = NotificationChannel(
                CHANNEL_ID_SYNC,
                "Background Cloud Synchronization",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps real-time Cloud Firestore updates and background push alerts active."
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(syncChannel)
        }
    }

    fun showSystemNotification(
        context: Context,
        title: String,
        body: String,
        kind: String = "admin",
        notificationId: Int = (System.currentTimeMillis() % 100000).toInt(),
        isCritical: Boolean = false
    ) {
        try {
            // Ensure notification channel is initialized
            createNotificationChannel(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("from_fcm", true)
                putExtra("fcm_kind", kind)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            val pendingIntent = PendingIntent.getActivity(context, notificationId, intent, flags)

            val channelId = if (isCritical) CHANNEL_ID_CRITICAL else CHANNEL_ID_HOSPITAL
            val priority = if (isCritical) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH
            val vibratePattern = if (isCritical) longArrayOf(0, 400, 200, 400, 200, 600) else longArrayOf(0, 250, 150, 250)
            val soundUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_stat_hospital)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(priority)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(if (isCritical) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_EVENT)
                .setVibrate(vibratePattern)
                .setSound(soundUri)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

            if (isCritical) {
                builder.setFullScreenIntent(pendingIntent, true)
            }

            // Post directly to Android system NotificationManager service
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying push notification: ${e.message}")
        }
    }

    /**
     * Helper to process any received notification message (from real FCM remote message
     * or triggered from in-app broadcast sender):
     * 1. Displays the heads-up Android system notification
     * 2. Inserts into the local Room database so in-app feed & unread badges update reactively
     */
    fun processIncomingMessage(
        context: Context,
        title: String,
        body: String,
        audience: String = "all",
        kind: String = "admin"
    ) {
        // 1. Show Android system notification
        showSystemNotification(context, title, body, kind)

        // 2. Persist to Room Database
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                val notif = NotificationEntity(
                    id = "fcm_" + UUID.randomUUID().toString().take(8),
                    title = title,
                    body = body,
                    time = timeFormat.format(Date()),
                    audience = audience,
                    read = false,
                    kind = kind
                )
                AppDatabase.getDatabase(context).hospitalDao().insertNotification(notif)
            } catch (e: Exception) {
                Log.e(TAG, "Failed inserting FCM message into database: ${e.message}")
            }
        }
    }
}
