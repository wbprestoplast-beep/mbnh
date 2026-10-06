package com.example.fcm

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
import com.google.firebase.messaging.FirebaseMessaging
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
    const val DEFAULT_TOPIC_BROADCAST = "hospital_broadcasts"
    const val DEFAULT_TOPIC_EMERGENCY = "hospital_emergencies"

    // Unique runtime ID for this phone instance to manage multi-device real-time sync
    val DEVICE_INSTANCE_ID: String = "dev_" + UUID.randomUUID().toString().take(8)

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
        createNotificationChannel(context)

        // Ensure FirebaseApp is initialized gracefully even if google-services.json was not provided
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:85326570714:android:com.aistudio.mbnursinghome.hmsapp")
                    .setProjectId("mb-nursing-home-hospital")
                    .setApiKey("AIzaSyDUMMY_KEY_FOR_LOCAL_FCM_INIT_78901")
                    .build()
                FirebaseApp.initializeApp(context.applicationContext, options)
                Log.d(TAG, "FirebaseApp initialized with fallback options")
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseApp initialization handled: ${e.message}")
        }

        // Fetch current token and subscribe to default hospital topics
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    updateToken(token)
                } else {
                    Log.w(TAG, "Fetching FCM token failed: ${task.exception?.message}")
                    if (_fcmToken.value == null) {
                        // Generate a local device simulation token if offline
                        _fcmToken.value = "fcm_local_${UUID.randomUUID().toString().take(12)}"
                    }
                }
            }

            // Subscribe to default broadcast channels
            subscribeToTopic(DEFAULT_TOPIC_BROADCAST)
            subscribeToTopic(DEFAULT_TOPIC_EMERGENCY)
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseMessaging service call error: ${e.message}")
            if (_fcmToken.value == null) {
                _fcmToken.value = "fcm_dev_mock_${UUID.randomUUID().toString().take(12)}"
            }
        }
    }

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // General Hospital Alerts Channel
            val generalChannel = NotificationChannel(
                CHANNEL_ID_HOSPITAL,
                "Hospital Alerts & Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for patient updates, routine tasks, and staff announcements."
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
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
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400, 200, 600)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(criticalChannel)
        }
    }

    fun subscribeToTopic(topic: String) {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _subscribedTopics.value = _subscribedTopics.value + topic
                        Log.d(TAG, "Subscribed to FCM topic: $topic")
                    } else {
                        Log.w(TAG, "Failed subscribing to topic $topic: ${task.exception?.message}")
                        // Keep optimistic state for local routing
                        _subscribedTopics.value = _subscribedTopics.value + topic
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseMessaging topic subscription error: ${e.message}")
            _subscribedTopics.value = _subscribedTopics.value + topic
        }
    }

    fun unsubscribeFromTopic(topic: String) {
        try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _subscribedTopics.value = _subscribedTopics.value - topic
                        Log.d(TAG, "Unsubscribed from FCM topic: $topic")
                    } else {
                        _subscribedTopics.value = _subscribedTopics.value - topic
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseMessaging topic unsubscription error: ${e.message}")
            _subscribedTopics.value = _subscribedTopics.value - topic
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

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(priority)
                .setCategory(if (isCritical) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_EVENT)
                .setVibrate(vibratePattern)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)

            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
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
