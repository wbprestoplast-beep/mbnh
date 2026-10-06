package com.example.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class HospitalFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("HospitalFCM", "New FCM token received: $token")
        FcmManager.updateToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("HospitalFCM", "FCM Message received from: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "MB Nursing Home Alert"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "New hospital update received."

        val audience = remoteMessage.data["audience"] ?: "all"
        val kind = remoteMessage.data["kind"] ?: "admin"

        FcmManager.processIncomingMessage(
            context = applicationContext,
            title = title,
            body = body,
            audience = audience,
            kind = kind
        )
    }
}
