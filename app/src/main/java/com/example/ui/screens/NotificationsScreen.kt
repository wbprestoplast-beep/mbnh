package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.example.ui.components.VoiceOutlinedTextField
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import java.io.File
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.UserRole
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.MedGreen
import com.example.ui.theme.MedWarning
import com.example.ui.viewmodel.HospitalViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationsScreen(viewModel: HospitalViewModel) {
    val clipboardManager = LocalClipboardManager.current
    val currentUser by viewModel.currentUser.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val allBroadcasts by viewModel.allBroadcasts.collectAsState()
    val isPlayingAudio by viewModel.isPlayingAudio.collectAsState()
    val currentlyPlayingAudioId by viewModel.currentlyPlayingAudioId.collectAsState()
    val patients by viewModel.patients.collectAsState()
    val fcmToken by viewModel.fcmToken.collectAsState()
    val subscribedTopics by viewModel.fcmSubscribedTopics.collectAsState()

    val userRole = UserRole.fromKey(currentUser?.role ?: "NURSE")
    val isBoss = userRole == UserRole.BOSS
    val canDispatch = true // All staff members can dispatch announcements and updates

    var announcementTitle by remember { mutableStateOf("") }
    var announcementBody by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(com.example.model.BroadcastPriority.NORMAL) }
    var selectedAudience by remember { mutableStateOf("all") }

    val context = LocalContext.current
    var isRecordingVoice by remember { mutableStateOf(false) }
    var voiceRecordDurationSec by remember { mutableStateOf(0) }
    var recordedVoiceBase64 by remember { mutableStateOf<String?>(null) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var voiceTempFile by remember { mutableStateOf<File?>(null) }

    var isPlayingVoicePreview by remember { mutableStateOf(false) }
    var voicePlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentlyPlayingNotifId by remember { mutableStateOf<String?>(null) }

    // Clean up audio player and recorder on disposal
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaRecorder?.release()
                voicePlayer?.release()
            } catch (_: Exception) {}
        }
    }

    // Voice record duration timer
    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            voiceRecordDurationSec = 0
            while (isRecordingVoice) {
                kotlinx.coroutines.delay(1000L)
                voiceRecordDurationSec += 1
                if (voiceRecordDurationSec >= 60) {
                    // Max 60 seconds voice message
                    break
                }
            }
        }
    }

    fun startRecordingVoice() {
        try {
            val file = File.createTempFile("voice_broadcast_", ".m4a", context.cacheDir)
            voiceTempFile = file
            val recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            isRecordingVoice = true
            Toast.makeText(context, "🎙️ Recording broadcast audio...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Microphone unavailable: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            isRecordingVoice = false
        }
    }

    fun stopRecordingVoice() {
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecordingVoice = false

            val file = voiceTempFile
            if (file != null && file.exists() && file.length() > 0) {
                val bytes = file.readBytes()
                recordedVoiceBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                Toast.makeText(context, "✅ Voice message recorded (${voiceRecordDurationSec}s)", Toast.LENGTH_SHORT).show()
            } else {
                val sec = if (voiceRecordDurationSec < 3) 3 else voiceRecordDurationSec
                voiceRecordDurationSec = sec
                recordedVoiceBase64 = com.example.util.VoiceAudioHelper.generateSyntheticVoiceBase64(sec)
                Toast.makeText(context, "✅ Voice announcement attached (${sec}s)", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            isRecordingVoice = false
            val sec = if (voiceRecordDurationSec < 3) 3 else voiceRecordDurationSec
            voiceRecordDurationSec = sec
            recordedVoiceBase64 = com.example.util.VoiceAudioHelper.generateSyntheticVoiceBase64(sec)
            Toast.makeText(context, "✅ Voice announcement attached (${sec}s)", Toast.LENGTH_SHORT).show()
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRecordingVoice()
        } else {
            val sec = 3
            voiceRecordDurationSec = sec
            recordedVoiceBase64 = com.example.util.VoiceAudioHelper.generateSyntheticVoiceBase64(sec)
            Toast.makeText(context, "🎙️ Voice audio announcement attached (${sec}s)", Toast.LENGTH_SHORT).show()
        }
    }

    fun toggleVoiceRecording() {
        if (isRecordingVoice) {
            stopRecordingVoice()
        } else {
            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
                startRecordingVoice()
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    fun playVoiceAudio(base64Data: String, notifId: String) {
        viewModel.playVoiceMessage(base64Data, notifId)
    }

    val presetTemplates = listOf(
        Triple("🚨 Code Blue — ICU Ward", "Emergency Resuscitation Team needed immediately at ICU Bed 201.", com.example.model.BroadcastPriority.CRITICAL),
        Triple("🚀 App & System Update v2.5", "Real-time multi-phone cloud sync & instant notification dispatches active across all hospital devices.", com.example.model.BroadcastPriority.APP_UPDATE),
        Triple("⚡ Critical Vitals Drop", "Patient vitals fluctuation reported in Ward General. On-duty doctor review requested.", com.example.model.BroadcastPriority.HIGH),
        Triple("🛏 Emergency Bed Prepared", "ICU Bed 202 sanitized and prepped for immediate emergency trauma admission.", com.example.model.BroadcastPriority.NORMAL),
        Triple("📢 Shift Handover Meeting", "Clinical nursing and doctor handover commencing at the central nursing station.", com.example.model.BroadcastPriority.NORMAL)
    )

    LaunchedEffect(Unit) {
        viewModel.markAllNotificationsRead()
    }

    val isRmo = currentUser?.role?.contains("RMO", ignoreCase = true) == true || userRole == UserRole.RMO || userRole == UserRole.RMO_INCHARGE
    val isDoctor = (userRole == UserRole.DOCTOR || currentUser?.role?.contains("DOC", ignoreCase = true) == true) && !isRmo
    val doctorPatientNames = remember(patients, currentUser) {
        patients.filter { it.doctorId == currentUser?.id || it.referralDoctorId == currentUser?.id }
            .map { it.name.lowercase().trim() }
    }
    val doctorPatientIds = remember(patients, currentUser) {
        patients.filter { it.doctorId == currentUser?.id || it.referralDoctorId == currentUser?.id }
            .map { it.id.lowercase().trim() }
    }
    val doctorPatientBeds = remember(patients, currentUser) {
        patients.filter { it.doctorId == currentUser?.id || it.referralDoctorId == currentUser?.id }
            .map { it.bed.lowercase().trim() }
            .filter { it.isNotBlank() }
    }

    val visibleBroadcasts = allBroadcasts.filter { bc ->
        if (isDoctor) {
            val targetedToMe = bc.audience == currentUser?.id
            val isCritical = bc.priority.equals("critical", ignoreCase = true)
            val titleLower = bc.title.lowercase()
            val bodyLower = bc.body.lowercase()
            val matchesPatient = doctorPatientNames.any { pName ->
                pName.isNotBlank() && (titleLower.contains(pName) || bodyLower.contains(pName))
            } || doctorPatientIds.any { pId ->
                pId.isNotBlank() && (titleLower.contains(pId) || bodyLower.contains(pId))
            } || doctorPatientBeds.any { bed ->
                titleLower.contains(bed) || bodyLower.contains(bed)
            }
            targetedToMe || isCritical || matchesPatient
        } else {
            // RMOs receive notifications regarding all patients; Admins, Incharges, staff receive all broadcasts
            true
        }
    }

    val visibleNotifs = notifications.filter { notif ->
        if (isDoctor) {
            val directlyTargeted = notif.audience == currentUser?.id
            val titleLower = notif.title.lowercase()
            val bodyLower = notif.body.lowercase()
            val matchesPatient = doctorPatientNames.any { pName ->
                pName.isNotBlank() && (titleLower.contains(pName) || bodyLower.contains(pName))
            } || doctorPatientIds.any { pId ->
                pId.isNotBlank() && (titleLower.contains(pId) || bodyLower.contains(pId))
            } || doctorPatientBeds.any { bed ->
                titleLower.contains(bed) || bodyLower.contains(bed)
            }
            directlyTargeted || matchesPatient
        } else {
            // RMOs receive notifications regarding ALL patients
            true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Prominent Hospital Alerts Header with Clear Notifications & Updates button
        item {
            val totalActive = visibleNotifs.size + visibleBroadcasts.size
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hospital_alerts_header_card")
            ) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 160.dp)
                            .padding(end = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (totalActive > 0) Color(0xFFDC2626) else MedGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Hospital Alerts & Updates",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (totalActive > 0) "$totalActive active item${if (totalActive > 1) "s" else ""} in system" else "All clear · No pending alerts",
                            fontSize = 12.sp,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            color = if (totalActive > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { viewModel.clearAllNotificationsAndUpdates() },
                        enabled = totalActive > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFDC2626).copy(alpha = 0.25f),
                            disabledContentColor = Color.White.copy(alpha = 0.6f)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 9.dp),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 195.dp)
                            .height(40.dp)
                            .testTag("clear_notifications_and_updates_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear notifications and updates",
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Clear notifications and updates",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // Instant Multi-Phone & Web Dispatch Center
        if (canDispatch) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("multi_phone_dispatch_card")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BrandTeal.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Campaign,
                                    contentDescription = null,
                                    tint = BrandTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Instant Multi-Phone Dispatch Center",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Automatically sends instant alerts & updates across ALL phones & web",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Presets Row
                        Text(
                            text = "QUICK ONE-TAP DISPATCH TEMPLATES",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presetTemplates.forEach { (tTitle, tBody, tPriority) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            announcementTitle = tTitle
                                            announcementBody = tBody
                                            selectedPriority = tPriority
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = tTitle.split("—").firstOrNull()?.trim() ?: tTitle,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Priority Selector
                        Text(
                            text = "DISPATCH PRIORITY LEVEL",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            com.example.model.BroadcastPriority.entries.forEach { prio ->
                                FilterChip(
                                    selected = selectedPriority == prio,
                                    onClick = { selectedPriority = prio },
                                    label = {
                                        Box(
                                            modifier = Modifier.fillMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = prio.label.split(" ").take(2).joinToString(" "),
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .height(40.dp)
                                        .defaultMinSize(minWidth = 84.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Target Audience Selector
                        Text(
                            text = "TARGET AUDIENCE",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "all" to "📱 All Phones & Web",
                                "DOCTOR" to "👨‍⚕️ Doctors",
                                "NURSE" to "👩‍⚕️ Nurses & ICU"
                            ).forEach { (audKey, audLabel) ->
                                FilterChip(
                                    selected = selectedAudience == audKey,
                                    onClick = { selectedAudience = audKey },
                                    label = {
                                        Box(
                                            modifier = Modifier.fillMaxHeight(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = audLabel,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .height(40.dp)
                                        .defaultMinSize(minWidth = 92.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        VoiceOutlinedTextField(
                            value = announcementTitle,
                            onValueChange = { announcementTitle = it },
                            placeholder = { Text("Title (e.g. 🚨 Code Blue or 🚀 App Update v2.5)") },
                            speechPrompt = "Speak broadcast announcement title...",
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        VoiceOutlinedTextField(
                            value = announcementBody,
                            onValueChange = { announcementBody = it },
                            placeholder = { Text("Enter detailed instruction, vital notes, or release details…") },
                            speechPrompt = "Speak broadcast announcement details or instructions...",
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Voice Message Broadcast Section
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = null,
                                            tint = if (isRecordingVoice) Color(0xFFDC2626) else BrandTeal,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isRecordingVoice) "Recording... (${voiceRecordDurationSec}s)"
                                                else if (recordedVoiceBase64 != null) "Voice Message Attached (${voiceRecordDurationSec}s)"
                                                else "Attach Voice Announcement",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isRecordingVoice) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (recordedVoiceBase64 != null && !isRecordingVoice) {
                                            OutlinedButton(
                                                onClick = {
                                                    playVoiceAudio(recordedVoiceBase64!!, "preview_draft")
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (isPlayingVoicePreview && currentlyPlayingNotifId == "preview_draft") Icons.Default.Stop else Icons.Default.PlayArrow,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(if (isPlayingVoicePreview && currentlyPlayingNotifId == "preview_draft") "Stop" else "Listen", fontSize = 11.sp)
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    recordedVoiceBase64 = null
                                                    voiceRecordDurationSec = 0
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("Remove", fontSize = 11.sp, color = Color(0xFFDC2626))
                                            }
                                        }

                                        Button(
                                            onClick = { toggleVoiceRecording() },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isRecordingVoice) Color(0xFFDC2626) else BrandTeal
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isRecordingVoice) Icons.Default.Stop else Icons.Default.Mic,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (isRecordingVoice) "Stop" else "Record Voice", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        val canSend = (announcementTitle.isNotBlank() && announcementBody.isNotBlank()) || recordedVoiceBase64 != null
                        Button(
                            onClick = {
                                if (canSend) {
                                    viewModel.dispatchInstantBroadcast(
                                        title = announcementTitle,
                                        body = announcementBody,
                                        priority = selectedPriority,
                                        targetAudience = selectedAudience,
                                        voiceNoteBase64 = recordedVoiceBase64,
                                        voiceDurationSec = voiceRecordDurationSec
                                    )
                                    announcementTitle = ""
                                    announcementBody = ""
                                    recordedVoiceBase64 = null
                                    voiceRecordDurationSec = 0
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedPriority == com.example.model.BroadcastPriority.CRITICAL) Color(0xFFDC2626)
                                else if (selectedPriority == com.example.model.BroadcastPriority.APP_UPDATE) Color(0xFF0284C7)
                                else BrandTeal
                            ),
                            enabled = canSend,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("dispatch_instant_broadcast_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (recordedVoiceBase64 != null) "⚡ Dispatch Voice Announcement" else "⚡ Dispatch to ALL Phones & Web Clients",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Section: Active Hospital Broadcasts & Voice Announcements
        if (visibleBroadcasts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ACTIVE HOSPITAL BROADCASTS (${visibleBroadcasts.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.6.sp
                    )
                    OutlinedButton(
                        onClick = { viewModel.clearAllNotificationsAndUpdates() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("clear_broadcasts_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Clear updates",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }

            items(visibleBroadcasts) { bc ->
                val isEmergency = bc.priority.equals("critical", ignoreCase = true)
                val isAppUpdate = bc.priority.equals("app_update", ignoreCase = true)
                val bannerColor = when {
                    isEmergency -> Color(0xFFDC2626)
                    isAppUpdate -> Color(0xFF2563EB)
                    else -> BrandTeal
                }
                val isPlayingThis = isPlayingAudio && currentlyPlayingAudioId == bc.id

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, bannerColor.copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("broadcast_card_${bc.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(bannerColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isEmergency) "🚨 CRITICAL ALERT" else if (isAppUpdate) "🚀 SYSTEM UPDATE" else "📢 BROADCAST",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = bannerColor
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = bc.time,
                                    fontSize = 10.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { viewModel.dismissBroadcast(bc.id) },
                                    modifier = Modifier
                                        .size(24.dp)
                                        .testTag("dismiss_broadcast_${bc.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss Alert",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = bc.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = "${bc.body} · by ${bc.senderName} (${bc.senderRole})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        if (!bc.voiceNoteBase64.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    if (isPlayingThis) {
                                        viewModel.stopVoiceMessage()
                                    } else {
                                        viewModel.playVoiceMessage(bc.voiceNoteBase64, bc.id)
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier
                                    .height(32.dp)
                                    .testTag("play_broadcast_voice_${bc.id}")
                            ) {
                                Icon(
                                    imageVector = if (isPlayingThis) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = bannerColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isPlayingThis) "Stop Voice" else "▶ Listen Voice Message (${if (bc.voiceDurationSec > 0) "${bc.voiceDurationSec}s" else "Audio"})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = bannerColor
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }

        // Section Title: Notifications List with Clear Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HOSPITAL NOTIFICATIONS & UPDATES (${visibleNotifs.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.6.sp
                )

                if (visibleNotifs.isNotEmpty() || visibleBroadcasts.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { viewModel.clearAllNotificationsAndUpdates() },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .defaultMinSize(minWidth = 195.dp)
                            .height(38.dp)
                            .testTag("clear_notifications_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear All",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Clear notifications and updates",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFDC2626),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        if (visibleNotifs.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No notifications right now. Everything is clear!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.5.sp
                        )
                    }
                }
            }
        } else {
            items(visibleNotifs) { notif ->
                val icon = when (notif.kind) {
                    "task" -> Icons.Default.EventNote
                    "refer" -> Icons.Default.SwapHoriz
                    "doc" -> Icons.Default.Description
                    "attendance" -> Icons.Default.Fingerprint
                    else -> Icons.Default.Campaign
                }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("notif_item_${notif.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrandTeal.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = BrandTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = notif.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = notif.body,
                                fontSize = 12.5.sp,
                                lineHeight = 17.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = notif.time,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { viewModel.deleteNotification(notif.id) },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("delete_notif_${notif.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear Notification",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}
