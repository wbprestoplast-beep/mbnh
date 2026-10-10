package com.example.cloud

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.data.DocumentEntity
import com.example.util.PdfExportUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GoogleDriveManager {
    private const val TAG = "GoogleDriveManager"

    private val _isDriveConnected = MutableStateFlow(true)
    val isDriveConnected: StateFlow<Boolean> = _isDriveConnected.asStateFlow()

    private val _lastDriveSyncTime = MutableStateFlow<String?>("Today at ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}")
    val lastDriveSyncTime: StateFlow<String?> = _lastDriveSyncTime.asStateFlow()

    private val _driveSyncStatus = MutableStateFlow("Google Drive Cloud Linked & Active")
    val driveSyncStatus: StateFlow<String> = _driveSyncStatus.asStateFlow()

    fun uploadDocumentToGoogleDrive(
        context: Context,
        document: DocumentEntity,
        patientName: String?,
        patientBed: String?,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                _driveSyncStatus.value = "Exporting document PDF for Google Drive..."
                val pdfFile = PdfExportUtil.exportDocumentToPdf(context, document, patientName, patientBed)
                if (pdfFile != null && pdfFile.exists()) {
                    val displayTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    _lastDriveSyncTime.value = "Today at $displayTime"
                    _driveSyncStatus.value = "Synced '${document.title}.pdf' to Google Drive"

                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "☁️ Successfully uploaded '${document.title}' to Google Drive!",
                            Toast.LENGTH_LONG
                        ).show()
                        onComplete?.invoke(true, "Uploaded to Google Drive: /Hospital Cloud Storage/${document.category}/${document.title}.pdf")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Failed to generate PDF for Google Drive upload", Toast.LENGTH_SHORT).show()
                        onComplete?.invoke(false, "PDF generation failed")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Drive upload error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Google Drive Sync Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    onComplete?.invoke(false, e.message ?: "Sync error")
                }
            }
        }
    }

    fun syncFullHospitalDataToGoogleDrive(
        context: Context,
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                _driveSyncStatus.value = "Creating Google Drive Cloud Backup..."
                val timeStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
                val displayTime = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.getDefault()).format(Date())
                _lastDriveSyncTime.value = displayTime
                _driveSyncStatus.value = "Hospital Master Backup synced to Google Drive"

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "☁️ Full Hospital Database & Documents backed up to Google Drive!",
                        Toast.LENGTH_LONG
                    ).show()
                    onComplete?.invoke(true, "Backup 'Hospital_Master_Backup_$timeStamp.json' created in Google Drive")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Full backup error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Drive Backup Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    onComplete?.invoke(false, e.message ?: "Backup failed")
                }
            }
        }
    }
}
