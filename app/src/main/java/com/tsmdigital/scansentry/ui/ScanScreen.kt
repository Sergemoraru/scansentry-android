package com.tsmdigital.scansentry.ui

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.annotation.OptIn
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.tsmdigital.scansentry.data.AppDatabase
import com.tsmdigital.scansentry.data.ScanRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun ScanScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val dao = remember { AppDatabase.get(context).scanDao() }
    val scope = rememberCoroutineScope()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    // Request permission on first entry
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var torchOn by remember { mutableStateOf(false) }
    var lastScan by remember { mutableStateOf<String?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var cooldownUntil by remember { mutableLongStateOf(0L) }

    // Camera handles we need to control torch/zoom.
    var camera by remember { mutableStateOf<Camera?>(null) }
    val scanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_AZTEC,
                Barcode.FORMAT_PDF417,
                Barcode.FORMAT_DATA_MATRIX,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
            )
            .build()
        BarcodeScanning.getClient(options)
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
            analysisExecutor.shutdown()
        }
    }

    fun persistScan(raw: String, format: String?, timestamp: Long) {
        scope.launch(Dispatchers.IO) {
            dao.insert(
                ScanRecord(
                    rawValue = raw,
                    format = format,
                    createdAtEpochMs = timestamp
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (!hasCameraPermission) {
            PermissionView(
                onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) }
            )
            return
        }

        // Full-bleed camera preview
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val provider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(Size(1280, 720))
                        .build()

                    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
                        val media = imageProxy.image
                        if (media == null) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val now = System.currentTimeMillis()
                        if (now < cooldownUntil) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val image = InputImage.fromMediaImage(media, imageProxy.imageInfo.rotationDegrees)

                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                val first = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                                if (first != null) {
                                    val raw = first.rawValue!!
                                    // Auto-zoom helper for small codes
                                    autoZoom(camera, previewView, first)

                                    // Simple dedupe + cooldown
                                    if (raw != lastScan) {
                                        lastScan = raw
                                        showResult = true
                                        cooldownUntil = now + 3_000

                                        persistScan(raw, first.format.toString(), now)
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    }

                    try {
                        provider.unbindAll()
                        val selector = CameraSelector.DEFAULT_BACK_CAMERA
                        camera = provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                    } catch (_: Throwable) {
                        // no-op
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Overlay: dim outside scan box + border
        val boxSize = 320.dp
        ScanOverlay(
            modifier = Modifier.fillMaxSize(),
            boxSize = boxSize
        )

        // Bottom-right torch
        IconButton(
            onClick = {
                torchOn = !torchOn
                camera?.cameraControl?.enableTorch(torchOn)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(56.dp)
                .background(Color(0x66FFFFFF), CircleShape)
        ) {
            Icon(
                imageVector = if (torchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                contentDescription = "Torch",
                tint = Color.White
            )
        }

        if (showResult && lastScan != null) {
            AlertDialog(
                onDismissRequest = { showResult = false },
                confirmButton = {
                    TextButton(onClick = { showResult = false }) { Text("Close") }
                },
                title = { Text("Scan Result") },
                text = { Text(lastScan ?: "") },
            )
        }
    }
}

@Composable
private fun PermissionView(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Camera permission required to scan.", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRequest) { Text("Allow Camera") }
    }
}

@Composable
private fun ScanOverlay(modifier: Modifier, boxSize: Dp) {
    Canvas(modifier = modifier) {
        val boxPx = boxSize.toPx()
        val left = (size.width - boxPx) / 2f
        val top = (size.height - boxPx) / 2f
        val rect = Rect(left, top, left + boxPx, top + boxPx)

        // Dim everything
        drawRect(Color.Black.copy(alpha = 0.35f))

        // Cut out a rounded rect hole
        val path = Path().apply {
            addRect(Rect(Offset.Zero, androidx.compose.ui.geometry.Size(size.width, size.height)))
            addRoundRect(RoundRect(rect, CornerRadius(20.dp.toPx(), 20.dp.toPx())))
            fillType = PathFillType.EvenOdd
        }
        drawPath(path, color = Color.Transparent, blendMode = BlendMode.Clear)

        // Border
        drawRoundRect(
            color = Color.White.copy(alpha = 0.9f),
            topLeft = Offset(rect.left, rect.top),
            size = androidx.compose.ui.geometry.Size(rect.width, rect.height),
            cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

private fun autoZoom(camera: Camera?, previewView: PreviewView, barcode: Barcode) {
    val cam = camera ?: return
    val box = barcode.boundingBox ?: return

    val viewW = previewView.width.toFloat().coerceAtLeast(1f)
    val viewH = previewView.height.toFloat().coerceAtLeast(1f)
    val viewArea = viewW * viewH
    val codeArea = (box.width().toFloat() * box.height().toFloat()).coerceAtLeast(1f)

    // Aim for ~22% of view area.
    val targetRatio = 0.22f
    val ratio = codeArea / viewArea
    val currentZoom = cam.cameraInfo.zoomState.value?.zoomRatio ?: 1f
    val scale = kotlin.math.sqrt((targetRatio / ratio).toDouble()).toFloat()
    val desired = (currentZoom * scale).coerceIn(1f, 6f)

    cam.cameraControl.setZoomRatio(desired)
}
