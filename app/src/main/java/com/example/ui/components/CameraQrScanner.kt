package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraQrScannerDialog(
    onDismiss: () -> Unit,
    onQrCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    BackHandler {
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            CameraPreviewAndScanner(
                onQrCodeScanned = onQrCodeScanned,
                onDismiss = onDismiss
            )
        } else {
            PermissionDeniedView(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                },
                onDismiss = onDismiss
            )
        }
    }
}

@Composable
fun CameraPreviewAndScanner(
    onQrCodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    // State to throttle scans and avoid multiple rapid triggers
    var isScanned by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            try {
                if (cameraProviderFuture.isDone) {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                }
            } catch (exc: Exception) {
                exc.printStackTrace()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor, QrCodeAnalyzer { result ->
                        if (!isScanned) {
                            isScanned = true
                            onQrCodeScanned(result)
                        }
                    })

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (exc: Exception) {
                        exc.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Custom Overlay Drawing the viewfinder and darkened surrounding areas
        ScannerOverlay()

        // Scan animation line
        ScanAnimationLine()

        // Controls Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Fechar Scanner",
                        tint = Color.White
                    )
                }

                Text(
                    text = "Escanear Código QR",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = {
                        val currentCamera = camera
                        if (currentCamera != null && currentCamera.cameraInfo.hasFlashUnit()) {
                            isTorchOn = !isTorchOn
                            currentCamera.cameraControl.enableTorch(isTorchOn)
                        }
                    },
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(50))
                        .size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Alternar Lanterna",
                        tint = if (isTorchOn) Color.Yellow else Color.White
                    )
                }
            }

            // Bottom text explanation
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Aponte a câmera para o QR code de 2 fatores (TOTP). Ele será detectado e importado automaticamente.",
                    color = Color.White,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ScannerOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Let's draw a centered viewfinder box (width=260dp equivalent)
        val boxSize = minOf(width * 0.7f, 280.dp.toPx())
        val left = (width - boxSize) / 2f
        val top = (height - boxSize) / 2f
        val rect = Rect(left, top, left + boxSize, top + boxSize)

        // Draw overlay with cutout path
        val overlayPath = Path().apply {
            addRect(Rect(0f, 0f, width, height))
        }
        val cutoutPath = Path().apply {
            addRoundRect(RoundRect(rect, CornerRadius(24.dp.toPx())))
        }

        clipPath(path = cutoutPath, clipOp = ClipOp.Difference) {
            drawPath(
                path = overlayPath,
                color = Color.Black.copy(alpha = 0.65f)
            )
        }

        // Draw neon rounded corners for viewfinder
        val cornerLength = 24.dp.toPx()
        val strokeWidth = 4.dp.toPx()
        val neonColor = Color(0xFF06B6D4) // Cosmic Cyan matching Professional Polish

        // Top-left corner
        drawLine(neonColor, Offset(left, top + cornerLength), Offset(left, top), strokeWidth)
        drawLine(neonColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth)

        // Top-right corner
        drawLine(neonColor, Offset(left + boxSize - cornerLength, top), Offset(left + boxSize, top), strokeWidth)
        drawLine(neonColor, Offset(left + boxSize, top), Offset(left + boxSize, top + cornerLength), strokeWidth)

        // Bottom-left corner
        drawLine(neonColor, Offset(left, top + boxSize - cornerLength), Offset(left, top + boxSize), strokeWidth)
        drawLine(neonColor, Offset(left, top + boxSize), Offset(left + cornerLength, top + boxSize), strokeWidth)

        // Bottom-right corner
        drawLine(neonColor, Offset(left + boxSize - cornerLength, top + boxSize), Offset(left + boxSize, top + boxSize), strokeWidth)
        drawLine(neonColor, Offset(left + boxSize, top + boxSize - cornerLength), Offset(left + boxSize, top + boxSize), strokeWidth)
    }
}

@Composable
fun ScanAnimationLine() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val boxSizeFraction = 0.7f
    
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val boxSize = minOf(width * boxSizeFraction, 280.dp.value * localDensity())
        val top = (height - boxSize) / 2f

        val localDensity = localDensity()

        val animatedY by infiniteTransition.animateFloat(
            initialValue = top + 10.dp.value * localDensity,
            targetValue = top + boxSize - 10.dp.value * localDensity,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scanY"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxWidth = minOf(size.width * boxSizeFraction, 280.dp.toPx())
            val left = (size.width - boxWidth) / 2f
            
            drawLine(
                color = Color(0xFF005AC1), // Polish Primary Blue laser line
                start = Offset(left + 16.dp.toPx(), animatedY),
                end = Offset(left + boxWidth - 16.dp.toPx(), animatedY),
                strokeWidth = 3.dp.toPx()
            )
        }
    }
}

@Composable
fun PermissionDeniedView(
    onRequestPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.PhotoCamera,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Permissão de Câmera Necessária",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Para poder ler códigos QR diretamente com a câmera do celular, precisamos de acesso à câmera.",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text(text = "Cancelar")
            }
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(text = "Permitir Acesso")
            }
        }
    }
}

// Density helpers to avoid Compose compilation errors
@Composable
private fun localDensity(): Float {
    return androidx.compose.ui.platform.LocalDensity.current.density
}

class QrCodeAnalyzer(
    private val onQrCodeDetected: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to arrayListOf(BarcodeFormat.QR_CODE)
        )
        setHints(hints)
    }

    override fun analyze(image: ImageProxy) {
        try {
            val yPlane = image.planes[0]
            val yBuffer = yPlane.buffer
            val rowStride = yPlane.rowStride
            val width = image.width
            val height = image.height
            
            // Allocate bytes for luminance plane
            val yBytes = ByteArray(width * height)
            for (y in 0 until height) {
                yBuffer.position(y * rowStride)
                yBuffer.get(yBytes, y * width, minOf(width, yBuffer.remaining()))
            }

            val source = PlanarYUVLuminanceSource(
                yBytes,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )

            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val result = reader.decode(binaryBitmap)
            onQrCodeDetected(result.text)
        } catch (e: Exception) {
            // Standard failure for frames with no decodable QR codes
        } finally {
            image.close()
        }
    }
}
