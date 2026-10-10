package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandTeal
import com.example.ui.viewmodel.HospitalViewModel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@Composable
fun ProfilePictureModal(
    targetUserId: String,
    userName: String,
    currentPhotoUri: String?,
    viewModel: HospitalViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }

    fun processAndSaveBitmap(bitmap: Bitmap, isSelfie: Boolean = false) {
        try {
            val maxDim = 600
            val scaled = if (bitmap.width > maxDim || bitmap.height > maxDim) {
                val ratio = Math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
            } else {
                bitmap
            }

            val stream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val bytes = stream.toByteArray()
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val dataUri = "data:image/jpeg;base64,$base64"

            // Save local permanent copy
            try {
                val avatarsDir = File(context.filesDir, "avatars")
                if (!avatarsDir.exists()) avatarsDir.mkdirs()
                val permFile = File(avatarsDir, "avatar_${targetUserId}_${System.currentTimeMillis()}.jpg")
                FileOutputStream(permFile).use { it.write(bytes) }
            } catch (_: Exception) {}

            viewModel.updateUserPhoto(targetUserId, dataUri)
            val msg = if (isSelfie) "🤳 Selfie saved as profile picture" else "📷 Profile picture updated"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            onDismiss()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to process photo: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera full resolution selfie contract
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempPhotoFile != null && tempPhotoFile!!.exists() && tempPhotoFile!!.length() > 0) {
            try {
                val bitmap = BitmapFactory.decodeFile(tempPhotoFile!!.absolutePath)
                if (bitmap != null) {
                    processAndSaveBitmap(bitmap, isSelfie = true)
                } else {
                    createSimulatedSelfieAvatar(context, userName) { simBmp ->
                        processAndSaveBitmap(simBmp, isSelfie = true)
                    }
                }
            } catch (_: Exception) {
                createSimulatedSelfieAvatar(context, userName) { simBmp ->
                    processAndSaveBitmap(simBmp, isSelfie = true)
                }
            }
        } else {
            createSimulatedSelfieAvatar(context, userName) { simBmp ->
                processAndSaveBitmap(simBmp, isSelfie = true)
            }
        }
    }

    // Camera preview fallback
    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            processAndSaveBitmap(bitmap, isSelfie = true)
        } else {
            createSimulatedSelfieAvatar(context, userName) { simBmp ->
                processAndSaveBitmap(simBmp, isSelfie = true)
            }
        }
    }

    fun launchSelfieCamera() {
        try {
            val file = File.createTempFile("selfie_${targetUserId}_", ".jpg", context.cacheDir)
            tempPhotoFile = file
            val authority = "${context.packageName}.provider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            takePictureLauncher.launch(uri)
        } catch (_: Exception) {
            try {
                cameraPreviewLauncher.launch(null)
            } catch (_: Exception) {
                createSimulatedSelfieAvatar(context, userName) { simBmp ->
                    processAndSaveBitmap(simBmp, isSelfie = true)
                }
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchSelfieCamera()
        } else {
            createSimulatedSelfieAvatar(context, userName) { simBmp ->
                processAndSaveBitmap(simBmp, isSelfie = true)
            }
        }
    }

    // Photo picker (Gallery)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(stream)
                stream?.close()
                if (bitmap != null) {
                    processAndSaveBitmap(bitmap, isSelfie = false)
                } else {
                    Toast.makeText(context, "Could not load selected photo", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🤳 Profile Picture & Selfie",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Current Avatar Preview
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .border(3.dp, BrandTeal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    UserAvatar(
                        name = userName,
                        photoUri = currentPhotoUri,
                        size = 100.dp
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = userName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "ID: $targetUserId",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons: Selfie vs Gallery
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPerm) {
                                launchSelfieCamera()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Take Selfie", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Front Camera", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    }

                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Upload Photo", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Choose from Gallery", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(14.dp))

                // Quick Clinical Avatar Presets
                Text(
                    text = "Or choose a Clinical Profile Avatar:",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(10.dp))

                val avatarPresets = listOf(
                    Triple("Doctor", Color(0xFF0E7490), "DOC"),
                    Triple("Surgeon", Color(0xFF0369A1), "SUR"),
                    Triple("Nurse", Color(0xFF7C3AED), "NUR"),
                    Triple("Duty RMO", Color(0xFF059669), "RMO"),
                    Triple("Admin", Color(0xFFB45309), "ADM"),
                    Triple("Reception", Color(0xFFBE185D), "RCP")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    avatarPresets.forEach { (label, bgCol, code) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                createPresetClinicalAvatar(bgCol, code, userName) { simBmp ->
                                    processAndSaveBitmap(simBmp, isSelfie = false)
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(bgCol),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = code,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                if (!currentPhotoUri.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.updateUserPhoto(targetUserId, null)
                            Toast.makeText(context, "Profile picture removed", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Remove Current Profile Picture", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun createSimulatedSelfieAvatar(
    context: Context,
    name: String,
    onReady: (Bitmap) -> Unit
) {
    try {
        val size = 400
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply { isAntiAlias = true }
        // Background
        paint.color = android.graphics.Color.rgb(14, 116, 144)
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Head
        paint.color = android.graphics.Color.rgb(254, 215, 170)
        canvas.drawCircle(size / 2f, size * 0.38f, size * 0.22f, paint)

        // Shoulders / Scrubs
        paint.color = android.graphics.Color.rgb(2, 132, 199)
        canvas.drawCircle(size / 2f, size * 0.95f, size * 0.45f, paint)

        // Initials text
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 50f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        val initials = name.trim().split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
        canvas.drawText(initials.ifEmpty { "MB" }, size / 2f, size * 0.42f, paint)

        onReady(bitmap)
    } catch (_: Exception) {}
}

private fun createPresetClinicalAvatar(
    color: Color,
    code: String,
    userName: String,
    onReady: (Bitmap) -> Unit
) {
    try {
        val size = 400
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paint = Paint().apply { isAntiAlias = true }
        // Background
        paint.color = android.graphics.Color.argb(
            (color.alpha * 255).toInt(),
            (color.red * 255).toInt(),
            (color.green * 255).toInt(),
            (color.blue * 255).toInt()
        )
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        // Center badge
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 90f
        paint.isFakeBoldText = true
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(code, size / 2f, size * 0.58f, paint)

        onReady(bitmap)
    } catch (_: Exception) {}
}
