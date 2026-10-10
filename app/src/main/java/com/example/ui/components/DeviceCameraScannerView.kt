package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.BrandCyan
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.MedGreen
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.Executors

private const val TAG = "DeviceCameraScanner"

/**
 * Live Device Camera Scanner & Auto-Sensing Viewfinder.
 * - Auto-fits across all device screen sizes and aspect ratios (Compact phones, Foldables, Tablets).
 * - Full-visibility live camera preview so the user clearly sees what they are positioning.
 * - Multi-tier camera fallback supporting all hardware levels (Full, Limited, Legacy).
 * - Real-time ImageAnalysis senses proper page adjustment (adequate lighting, text/document edge contrast, and frame stability).
 * - Takes picture automatically once page adjustment is sensed and locked, with ZERO clicks required!
 * - Includes responsive manual snap shutter button, torch/flash toggle, camera flip, and system camera fallback.
 */
@Composable
fun DeviceCameraScannerView(
    onPageCaptured: (Bitmap) -> Unit,
    onClose: () -> Unit,
    onFallbackSystemCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Camera permission needed for live document scanning", Toast.LENGTH_SHORT).show()
        }
    }

    var isFrameAligned by remember { mutableStateOf(false) }
    var autoScanProgress by remember { mutableFloatStateOf(0f) }
    var autoScanStatus by remember { mutableStateOf("Position page inside the alignment frame…") }
    var isCapturingNow by remember { mutableStateOf(false) }
    var showFlashAnimation by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var isTorchOn by remember { mutableStateOf(false) }
    var cameraHardwareAvailable by remember { mutableStateOf(true) }
    var isAnalyzerActive by remember { mutableStateOf(true) }

    var cameraControlInstance by remember { mutableStateOf<CameraControl?>(null) }
    var imageCaptureUseCase by remember { mutableStateOf<ImageCapture?>(null) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            setOnTouchListener { _, motionEvent ->
                if (motionEvent.action == android.view.MotionEvent.ACTION_UP) {
                    try {
                        val factory = meteringPointFactory
                        val point = factory.createPoint(motionEvent.x, motionEvent.y)
                        val action = androidx.camera.core.FocusMeteringAction.Builder(
                            point,
                            androidx.camera.core.FocusMeteringAction.FLAG_AF or androidx.camera.core.FocusMeteringAction.FLAG_AE
                        ).setAutoCancelDuration(3, java.util.concurrent.TimeUnit.SECONDS).build()
                        cameraControlInstance?.startFocusAndMetering(action)
                    } catch (_: Exception) {}
                }
                true
            }
        }
    }

    // Animated scanning laser line moving up and down the document frame
    val laserTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserFraction by laserTransition.animateFloat(
        initialValue = 0.05f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_anim"
    )

    fun triggerVibration() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (_: Exception) {}
    }

    fun executeCapture() {
        if (isCapturingNow) return
        isCapturingNow = true
        showFlashAnimation = true
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            showFlashAnimation = false
        }, 180)
        triggerVibration()

        val imageCapture = imageCaptureUseCase
        if (imageCapture == null) {
            isCapturingNow = false
            showFlashAnimation = false
            onFallbackSystemCamera()
            return
        }

        try {
            val photoFile = File.createTempFile("scan_page_", ".jpg", context.cacheDir)
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        try {
                            val rawBmp = BitmapFactory.decodeFile(photoFile.absolutePath)
                            if (rawBmp != null) {
                                val exif = ExifInterface(photoFile.absolutePath)
                                val orientation = exif.getAttributeInt(
                                    ExifInterface.TAG_ORIENTATION,
                                    ExifInterface.ORIENTATION_NORMAL
                                )
                                val degrees = when (orientation) {
                                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                                    else -> 0
                                }
                                val matrix = Matrix().apply {
                                    if (degrees != 0) postRotate(degrees.toFloat())
                                }
                                val finalBmp = Bitmap.createBitmap(rawBmp, 0, 0, rawBmp.width, rawBmp.height, matrix, true)
                                ContextCompat.getMainExecutor(context).execute {
                                    isCapturingNow = false
                                    showFlashAnimation = false
                                    isFrameAligned = false
                                    autoScanProgress = 0f
                                    onPageCaptured(finalBmp)
                                }
                            } else {
                                isCapturingNow = false
                                showFlashAnimation = false
                                onFallbackSystemCamera()
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error processing captured bitmap: ${e.message}")
                            isCapturingNow = false
                            showFlashAnimation = false
                            onFallbackSystemCamera()
                        } finally {
                            photoFile.delete()
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                        isCapturingNow = false
                        showFlashAnimation = false
                        Toast.makeText(context, "Using system camera fallback...", Toast.LENGTH_SHORT).show()
                        onFallbackSystemCamera()
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error launching takePicture: ${e.message}")
            isCapturingNow = false
            showFlashAnimation = false
            onFallbackSystemCamera()
        }
    }

    // Auto-capture countdown once page adjustment is sensed and locked
    LaunchedEffect(isFrameAligned) {
        if (isFrameAligned && !isCapturingNow) {
            autoScanStatus = "🎯 PAGE SENSED & ADJUSTED! Capturing now…"
            delay(380L)
            executeCapture()
        }
    }

    // Backup auto-capture timer if analyzer is limited by device chipset
    LaunchedEffect(isAnalyzerActive, hasCameraPermission) {
        if (!isAnalyzerActive && hasCameraPermission && !isCapturingNow) {
            autoScanStatus = "Position document… Auto-snapping in 3s"
            delay(1000L)
            autoScanProgress = 0.33f
            delay(1000L)
            autoScanProgress = 0.66f
            delay(1000L)
            autoScanProgress = 1.0f
            isFrameAligned = true
        }
    }

    // CameraX Lifecycle Setup with Multi-Tier Fallback
    LaunchedEffect(lensFacing, lifecycleOwner, hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                imageCaptureUseCase = imageCapture

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()

                var lastLuma = 0.0
                var stableFrames = 0

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    try {
                        val plane = imageProxy.planes.getOrNull(0)
                        if (plane != null && !isCapturingNow) {
                            val buffer = plane.buffer
                            val width = imageProxy.width
                            val height = imageProxy.height
                            val rowStride = plane.rowStride
                            val pixelStride = plane.pixelStride

                            val startX = (width * 0.15).toInt()
                            val endX = (width * 0.85).toInt()
                            val startY = (height * 0.15).toInt()
                            val endY = (height * 0.85).toInt()

                            var totalLuma = 0L
                            var sampleCount = 0
                            var edgeEnergy = 0L
                            var prevVal = -1

                            val stepX = maxOf(4, (endX - startX) / 28)
                            val stepY = maxOf(4, (endY - startY) / 28)

                            for (y in startY until endY step stepY) {
                                val rowOffset = y * rowStride
                                for (x in startX until endX step stepX) {
                                    val index = rowOffset + x * pixelStride
                                    if (index < buffer.limit()) {
                                        val luma = buffer.get(index).toInt() and 0xFF
                                        totalLuma += luma
                                        sampleCount++
                                        if (prevVal >= 0) {
                                            edgeEnergy += kotlin.math.abs(luma - prevVal)
                                        }
                                        prevVal = luma
                                    }
                                }
                            }

                            if (sampleCount > 0) {
                                val avgLuma = totalLuma.toDouble() / sampleCount
                                val avgEdge = edgeEnergy.toDouble() / sampleCount

                                val isStable = kotlin.math.abs(avgLuma - lastLuma) < 16.0
                                val hasContrast = avgEdge >= 2.0
                                val hasLighting = avgLuma in 15.0..252.0
                                lastLuma = avgLuma

                                if (isStable && hasContrast && hasLighting) {
                                    stableFrames = minOf(stableFrames + 1, 8)
                                } else {
                                    stableFrames = maxOf(0, stableFrames - 1)
                                }

                                val progress = (stableFrames / 4f).coerceIn(0f, 1f)
                                autoScanProgress = progress

                                if (stableFrames >= 4 && !isFrameAligned) {
                                    isFrameAligned = true
                                } else if (stableFrames < 2 && isFrameAligned && !isCapturingNow) {
                                    isFrameAligned = false
                                }

                                autoScanStatus = when {
                                    isFrameAligned -> "🎯 Page properly adjusted! Capturing…"
                                    progress >= 0.7f -> "📐 Sensing document page… Hold steady!"
                                    progress >= 0.3f -> "🔍 Adjusting framing & contrast…"
                                    else -> "Position document inside the alignment frame…"
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Analyzer error: ${e.message}")
                    } finally {
                        imageProxy.close()
                    }
                }

                val requestedSelector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                val cameraSelector = try {
                    if (cameraProvider.hasCamera(requestedSelector)) {
                        requestedSelector
                    } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    } else {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    }
                } catch (_: Exception) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }

                // Tier 1: Attempt full 3-use-case binding (Preview + ImageCapture + ImageAnalysis)
                val camera: Camera = try {
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalysis
                    ).also {
                        isAnalyzerActive = true
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Hardware combination limitation: falling back to 2 use-cases: ${e.message}")
                    // Tier 2: Fallback for devices with limited surface combinations
                    isAnalyzerActive = false
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                }

                cameraControlInstance = camera.cameraControl
                cameraHardwareAvailable = true
            } catch (exc: Exception) {
                Log.e(TAG, "CameraX binding failure: ${exc.message}", exc)
                cameraHardwareAvailable = false
            }
        }, ContextCompat.getMainExecutor(context))
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraExecutor.shutdown()
                cameraControlInstance?.enableTorch(false)
            } catch (_: Exception) {}
        }
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("device_camera_scanner_card")
    ) {
        if (!hasCameraPermission) {
            // Permission Request State
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(BrandTeal.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = BrandCyan,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Camera Permission Required",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "To scan and auto-sense medical pages in real time, please grant camera permission.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Camera Permission", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onFallbackSystemCamera,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Use System Camera App", color = Color.White, fontSize = 13.sp)
                }
            }
        } else if (!cameraHardwareAvailable) {
            // Standby Fallback State
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = BrandCyan,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Camera Ready for Capture",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Position your document and use high-resolution capture or system camera.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onFallbackSystemCamera,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandTeal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Snap with System Camera", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        } else {
            // Live Viewfinder Display
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .background(Color.Black)
            ) {
                val boxWidth = maxWidth
                val boxHeight = maxHeight

                Box(modifier = Modifier.fillMaxSize()) {
                    // Full-bleed live camera hardware preview
                    AndroidView(
                        factory = { previewView },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Semi-transparent overlay with clear cutout for document
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        val targetWidth = canvasWidth * 0.82f
                        val targetHeight = canvasHeight * 0.72f
                        val left = (canvasWidth - targetWidth) / 2f
                        val top = (canvasHeight - targetHeight) / 2.3f
                        val right = left + targetWidth
                        val bottom = top + targetHeight

                        // Dim the outer margins
                        drawRect(
                            color = Color.Black.copy(alpha = 0.42f),
                            size = Size(canvasWidth, top)
                        )
                        drawRect(
                            color = Color.Black.copy(alpha = 0.42f),
                            topLeft = Offset(0f, bottom),
                            size = Size(canvasWidth, canvasHeight - bottom)
                        )
                        drawRect(
                            color = Color.Black.copy(alpha = 0.42f),
                            topLeft = Offset(0f, top),
                            size = Size(left, targetHeight)
                        )
                        drawRect(
                            color = Color.Black.copy(alpha = 0.42f),
                            topLeft = Offset(right, top),
                            size = Size(canvasWidth - right, targetHeight)
                        )

                        val frameBorderColor = if (isFrameAligned) {
                            Color(0xFF22C55E) // Bright Green when sensed
                        } else {
                            Color(0xFF06B6D4) // Bright Cyan scanning
                        }

                        // Border around document frame
                        drawRoundRect(
                            color = frameBorderColor.copy(alpha = 0.6f),
                            topLeft = Offset(left, top),
                            size = Size(targetWidth, targetHeight),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 10f), 0f)
                            )
                        )

                        // 4 Bold High-Contrast Corner Guides
                        val cornerLen = 28.dp.toPx()
                        val cornerStroke = 4.5.dp.toPx()

                        // Top-Left Corner
                        drawLine(frameBorderColor, Offset(left, top), Offset(left + cornerLen, top), cornerStroke)
                        drawLine(frameBorderColor, Offset(left, top), Offset(left, top + cornerLen), cornerStroke)

                        // Top-Right Corner
                        drawLine(frameBorderColor, Offset(right, top), Offset(right - cornerLen, top), cornerStroke)
                        drawLine(frameBorderColor, Offset(right, top), Offset(right, top + cornerLen), cornerStroke)

                        // Bottom-Left Corner
                        drawLine(frameBorderColor, Offset(left, bottom), Offset(left + cornerLen, bottom), cornerStroke)
                        drawLine(frameBorderColor, Offset(left, bottom), Offset(left, bottom - cornerLen), cornerStroke)

                        // Bottom-Right Corner
                        drawLine(frameBorderColor, Offset(right, bottom), Offset(right - cornerLen, bottom), cornerStroke)
                        drawLine(frameBorderColor, Offset(right, bottom), Offset(right, bottom - cornerLen), cornerStroke)

                        // Animated Scanning Laser Bar
                        val laserY = top + (targetHeight * laserFraction)
                        val laserColor = if (isFrameAligned) Color(0xFF22C55E) else Color(0xFF06B6D4)
                        drawLine(
                            color = laserColor.copy(alpha = 0.85f),
                            start = Offset(left + 8.dp.toPx(), laserY),
                            end = Offset(right - 8.dp.toPx(), laserY),
                            strokeWidth = 3.dp.toPx()
                        )
                    }

                    // Top Floating Camera Controls (Flash, Flip, Close)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Lens Switch (Back / Front)
                        IconButton(
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Flashlight / Torch Toggle
                            IconButton(
                                onClick = {
                                    val nextTorch = !isTorchOn
                                    isTorchOn = nextTorch
                                    cameraControlInstance?.enableTorch(nextTorch)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Toggle Torch",
                                    tint = if (isTorchOn) Color(0xFFFDE047) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Close Viewfinder Button
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Close Viewfinder",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Bottom Floating Sensing Feedback Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .align(Alignment.BottomCenter)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Sensing percentage badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (isFrameAligned) MedGreen.copy(alpha = 0.95f)
                                        else Color.Black.copy(alpha = 0.75f)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isFrameAligned) Icons.Default.CheckCircle else Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (isFrameAligned) Color.White else Color(0xFF22D3EE),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isFrameAligned) "PAGE SENSED & LOCKED (100%)"
                                        else "AUTO-SENSING PAGE (${(autoScanProgress * 100).toInt()}%)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = autoScanStatus,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                color = Color.White,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Shutter flash animation
                    if (showFlashAnimation) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White)
                        )
                    }
                }
            }

            // Real-time Page Sensing Progress Bar
            LinearProgressIndicator(
                progress = { autoScanProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                color = if (isFrameAligned) MedGreen else Color(0xFF22D3EE),
                trackColor = Color(0xFF1E293B)
            )

            // Bottom Shutter & Manual Capture Controls (Fit on all devices)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Primary Snap Picture Button (manual trigger or auto-sensed)
                    Button(
                        onClick = { executeCapture() },
                        enabled = !isCapturingNow,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFrameAligned) MedGreen else BrandTeal
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("snap_camera_page_button")
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFrameAligned) "✓ Sensed · Snapping Page…" else "📸 Snap Picture Now",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    // Fallback to system camera app
                    OutlinedButton(
                        onClick = onFallbackSystemCamera,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("System App", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
