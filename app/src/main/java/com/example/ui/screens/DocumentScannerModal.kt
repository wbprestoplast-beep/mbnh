package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.model.DocCategory
import com.example.model.DocumentFilter
import com.example.ui.components.DocumentCanvasView
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandTeal
import com.example.ui.viewmodel.HospitalViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentScannerModal(viewModel: HospitalViewModel) {
    val context = LocalContext.current
    val patients by viewModel.patients.collectAsState()
    val targetPatientId by viewModel.scannerTargetPatientId.collectAsState()

    var selectedPatientId by remember {
        mutableStateOf(targetPatientId ?: patients.firstOrNull()?.id ?: "")
    }
    var selectedCategory by remember { mutableStateOf(DocCategory.PRESCRIPTION.label) }
    var documentTitle by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedUriString by remember { mutableStateOf<String?>(null) }
    var activeFilter by remember { mutableStateOf(DocumentFilter.ORIGINAL) }

    var isPatientMenuExpanded by remember { mutableStateOf(false) }
    var isCategoryMenuExpanded by remember { mutableStateOf(false) }

    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var tempPhotoFile by remember { mutableStateOf<File?>(null) }

    // Full-resolution camera capture contract
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempPhotoFile != null && tempPhotoFile!!.exists()) {
            capturedUriString = tempPhotoFile!!.absolutePath
            try {
                capturedBitmap = BitmapFactory.decodeFile(tempPhotoFile!!.absolutePath)
            } catch (_: Exception) {}
        }
    }

    // Camera launcher fallback (preview)
    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            try {
                val tempFile = File.createTempFile("scan_", ".jpg", context.cacheDir)
                tempFile.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                }
                capturedUriString = tempFile.absolutePath
            } catch (_: Exception) {}
        }
    }

    fun launchCameraDirectly() {
        try {
            val file = File.createTempFile("med_doc_", ".jpg", context.cacheDir)
            tempPhotoFile = file
            val authority = "${context.packageName}.provider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            tempPhotoUri = uri
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            // Fallback to preview contract if FileProvider or intent creation fails
            try {
                cameraPreviewLauncher.launch(null)
            } catch (e2: Exception) {
                Toast.makeText(context, "Camera unavailable on this device: ${e2.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Runtime permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraDirectly()
        } else {
            Toast.makeText(context, "Camera permission is required to scan reports", Toast.LENGTH_LONG).show()
        }
    }

    fun onCameraClick() {
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            launchCameraDirectly()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            capturedUriString = uri.toString()
            capturedBitmap = null
        }
    }

    val hasImage = capturedBitmap != null || capturedUriString != null

    Dialog(onDismissRequest = { viewModel.closeScanner() }) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📷 Scan Medical Document",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { viewModel.closeScanner() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (!hasImage) {
                    // Initial capture options
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onCameraClick() },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                            contentPadding = PaddingValues(vertical = 20.dp, horizontal = 12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Camera", fontWeight = FontWeight.Bold)
                                Text("Capture with shutter", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
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
                            contentPadding = PaddingValues(vertical = 20.dp, horizontal = 12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Photo Picker", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Import from gallery", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Or choose one of the pre-configured sample documents:",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("Prescription", "SEED_RX", DocCategory.PRESCRIPTION.label),
                            Triple("Lab Panel", "SEED_LAB", DocCategory.LAB_PANEL.label),
                            Triple("X-Ray", "SEED_XRAY", DocCategory.X_RAY.label),
                            Triple("MRI Scan", "SEED_MRI", DocCategory.MRI_CT.label)
                        ).forEach { (lbl, seedType, cat) ->
                            OutlinedButton(
                                onClick = {
                                    capturedUriString = seedType
                                    selectedCategory = cat
                                    documentTitle = "$lbl — Sample"
                                },
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(lbl, fontSize = 10.5.sp, maxLines = 1)
                            }
                        }
                    }
                } else {
                    // Preview with filters
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                    ) {
                        if (capturedBitmap != null) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Scanned document",
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else if (capturedUriString != null) {
                            DocumentCanvasView(
                                docTypeOrUri = capturedUriString!!,
                                filter = activeFilter,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DocumentFilter.entries.forEach { f ->
                            FilterChip(
                                selected = activeFilter == f,
                                onClick = { activeFilter = f },
                                label = { Text(f.label, fontSize = 10.sp) },
                                shape = RoundedCornerShape(999.dp)
                            )
                        }
                        IconButton(onClick = {
                            capturedBitmap = null
                            capturedUriString = null
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Retake")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Patient Selector Dropdown
                    Text("ASSIGN TO PATIENT", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ExposedDropdownMenuBox(
                        expanded = isPatientMenuExpanded,
                        onExpandedChange = { isPatientMenuExpanded = !isPatientMenuExpanded }
                    ) {
                        val pt = patients.firstOrNull { it.id == selectedPatientId }
                        OutlinedTextField(
                            value = pt?.let { "${it.name} — ${it.id} (${it.bed})" } ?: "Select patient",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isPatientMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = isPatientMenuExpanded,
                            onDismissRequest = { isPatientMenuExpanded = false }
                        ) {
                            patients.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.name} — ${p.id} (${p.bed})") },
                                    onClick = {
                                        selectedPatientId = p.id
                                        isPatientMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Document Category Dropdown
                    Text("DOCUMENT CATEGORY", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ExposedDropdownMenuBox(
                        expanded = isCategoryMenuExpanded,
                        onExpandedChange = { isCategoryMenuExpanded = !isCategoryMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = isCategoryMenuExpanded,
                            onDismissRequest = { isCategoryMenuExpanded = false }
                        ) {
                            DocCategory.entries.forEach { c ->
                                DropdownMenuItem(
                                    text = { Text(c.label) },
                                    onClick = {
                                        selectedCategory = c.label
                                        isCategoryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = documentTitle,
                        onValueChange = { documentTitle = it },
                        label = { Text("Document Title (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        label = { Text("Clinical Remarks / Notes") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val uri = capturedUriString ?: "SEED_RX"
                            viewModel.saveDocument(
                                patientId = selectedPatientId,
                                title = documentTitle.trim(),
                                category = selectedCategory,
                                docTypeOrUri = uri,
                                filter = activeFilter,
                                remarks = remarks.trim()
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                        enabled = selectedPatientId.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.Upload, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save to Medical Record", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
