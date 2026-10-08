package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Boot and system update receiver.
 * Automatically initiates HospitalSyncService in the background so that
 * multi-device cloud synchronization and emergency alerts run even before
 * the user manually opens the application.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "Received system broadcast: $action. Starting HospitalSyncService in background.")
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            HospitalSyncService.start(context)
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
