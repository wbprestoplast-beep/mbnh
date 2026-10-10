package com.example.service

/**
 * Tracks whether the hospital application is active in the foreground or running in the background.
 * Ensures system heads-up push notifications are delivered when the app is minimized or backgrounded.
 */
object AppLifecycleTracker {
    @Volatile
    var isAppInForeground: Boolean = false
}
