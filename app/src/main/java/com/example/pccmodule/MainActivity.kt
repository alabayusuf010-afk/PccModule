package com.example.pccmodule

import android.annotation.SuppressLint
import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.pccmodule.ui.theme.PccModuleTheme
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    private lateinit var analysisExecutor: ExecutorService
    private val pccModule = PccModule()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        analysisExecutor = Executors.newSingleThreadExecutor()
        enableEdgeToEdge()
        setContent {
            PccModuleTheme {
                MainScreen(pccModule, analysisExecutor)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        analysisExecutor.shutdown()
    }
}

@Composable
fun MainScreen(pccModule: PccModule, executor: ExecutorService) {
    val context = LocalContext.current
    
    var r1 by remember { mutableDoubleStateOf(1.0) }
    var status by remember { mutableStateOf("IDLE") }
    var discardRate by remember { mutableDoubleStateOf(0.0) }
    var alertColor by remember { mutableStateOf("GREEN") }
    var theta by remember { mutableFloatStateOf(0.85f) }
    var showRoi by remember { mutableStateOf(true) }
    var showIta by remember { mutableStateOf(false) }
    var itaIterations by remember { mutableIntStateOf(0) }
    var roiPoints by remember { mutableStateOf(listOf<PccModule.Point>()) }
    var itaPoints by remember { mutableStateOf(listOf<PccModule.Point>()) }
    var isDpmActive by remember { mutableStateOf(true) }
    var lastProcTime by remember { mutableStateOf(0.0) }
    var fps by remember { mutableLongStateOf(0L) }
    var frameCount by remember { mutableIntStateOf(0) }
    var lastFpsTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    var recording: Recording? by remember { mutableStateOf(null) }
    var isRecording by remember { mutableStateOf(false) }
    val videoCaptureState = remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    val imageCaptureState = remember { mutableStateOf<ImageCapture?>(null) }

    val hasPermissions = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions.value = permissions.values.all { it }
    }

    LaunchedEffect(Unit) {
        if (!hasPermissions.value) {
            launcher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        if (hasPermissions.value) {
            Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                CameraPreview(
                    pccModule = pccModule,
                    executor = executor,
                    isDpmActive = isDpmActive,
                    onVideoCaptureReady = { videoCaptureState.value = it },
                    onImageCaptureReady = { imageCaptureState.value = it },
                    onUpdate = {
                        r1 = pccModule.r1
                        status = pccModule.status
                        discardRate = pccModule.discardRate
                        alertColor = pccModule.creAlert
                        roiPoints = pccModule.roiPoints.toList()
                        itaPoints = pccModule.itaPoints.toList()
                        itaIterations = pccModule.itaIterations
                        lastProcTime = pccModule.lastProcessingTimeNs / 1_000_000.0
                        
                        // FPS Logic
                        frameCount++
                        val now = System.currentTimeMillis()
                        if (now - lastFpsTimestamp >= 1000) {
                            fps = frameCount.toLong()
                            frameCount = 0
                            lastFpsTimestamp = now
                        }
                    }
                )

                if (showRoi) {
                    PointsOverlay(roiPoints, Color.Red)
                }
                if (showIta) {
                    PointsOverlay(itaPoints, Color.Cyan)
                }

                // Capture Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Button(onClick = {
                        takePhoto(context, imageCaptureState.value)
                    }) {
                        Text("Photo")
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (isRecording) {
                                recording?.stop()
                                recording = null
                                isRecording = false
                            } else {
                                recording = startRecording(context, videoCaptureState.value) {
                                    isRecording = false
                                    recording = null
                                }
                                isRecording = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRecording) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(if (isRecording) "Stop Rec" else "Record Video")
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        pccModule.logExperimentData()
                        Toast.makeText(context, "Data logged to Logcat (PCC_EXPERIMENT)", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Log Data")
                    }
                }

                DebugPanel(
                    modifier = Modifier.align(Alignment.BottomCenter),
                    r1 = r1,
                    status = status,
                    discardRate = discardRate,
                    alertColor = alertColor,
                    theta = theta,
                    onThetaChange = { 
                        theta = it
                        pccModule.setTheta(it.toDouble())
                    },
                    isDpmActive = isDpmActive,
                    onDpmToggle = { isDpmActive = it },
                    showRoi = showRoi,
                    onRoiToggle = { showRoi = it },
                    showIta = showIta,
                    onItaToggle = { showIta = it },
                    itaIterations = itaIterations,
                    lastProcTime = lastProcTime,
                    fps = fps
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Permissions required")
            }
        }
    }
}

@Composable
fun CameraPreview(
    pccModule: PccModule,
    executor: ExecutorService,
    isDpmActive: Boolean,
    onVideoCaptureReady: (VideoCapture<Recorder>) -> Unit,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onUpdate: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()
            val videoCapture = VideoCapture.withOutput(recorder)
            onVideoCaptureReady(videoCapture)

            val imageCapture = ImageCapture.Builder().build()
            onImageCaptureReady(imageCapture)

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()

            imageAnalysis.setAnalyzer(executor) { imageProxy ->
                    val bitmap = imageProxy.toGrayscaleBitmap()
                    if (isDpmActive) {
                        pccModule.processFrame(bitmap)
                    } else {
                        val currentTheta = pccModule.theta
                        pccModule.setTheta(2.0) // Force processing (r1 is always <= 1.0, so r1 > 2.0 is false)
                        pccModule.processFrame(bitmap)
                        pccModule.setTheta(currentTheta)
                    }
                    onUpdate()
                    imageProxy.close()
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis,
                    videoCapture,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e("CameraPreview", "Binding failed", e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
}

private fun takePhoto(context: android.content.Context, imageCapture: ImageCapture?) {
    if (imageCapture == null) return

    val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PccModule")
        }
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                Toast.makeText(context, "Photo saved", Toast.LENGTH_SHORT).show()
            }
            override fun onError(exception: ImageCaptureException) {
                Log.e("MainActivity", "Photo capture failed: ${exception.message}", exception)
            }
        }
    )
}

@SuppressLint("MissingPermission")
private fun startRecording(
    context: android.content.Context, 
    videoCapture: VideoCapture<Recorder>?,
    onFinished: () -> Unit
): Recording? {
    if (videoCapture == null) return null

    val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/PccModule")
        }
    }

    val mediaStoreOutputOptions = MediaStoreOutputOptions
        .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        .setContentValues(contentValues)
        .build()

    return videoCapture.output
        .prepareRecording(context, mediaStoreOutputOptions)
        .withAudioEnabled()
        .start(ContextCompat.getMainExecutor(context)) { recordEvent ->
            when(recordEvent) {
                is VideoRecordEvent.Finalize -> {
                    if (!recordEvent.hasError()) {
                        Toast.makeText(context, "Video saved", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e("MainActivity", "Video recording error: ${recordEvent.error}")
                    }
                    onFinished()
                }
            }
        }
}

@Composable
fun PointsOverlay(points: List<PccModule.Point>, color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        points.forEach { point ->
            val scaleX = size.width / 480f 
            val scaleY = size.height / 640f
            drawCircle(color, radius = 6f, center = Offset(point.x.toFloat() * scaleX, point.y.toFloat() * scaleY))
        }
    }
}

@Composable
fun DebugPanel(
    modifier: Modifier = Modifier,
    r1: Double,
    status: String,
    discardRate: Double,
    alertColor: String,
    theta: Float,
    onThetaChange: (Float) -> Unit,
    isDpmActive: Boolean,
    onDpmToggle: (Boolean) -> Unit,
    showRoi: Boolean,
    onRoiToggle: (Boolean) -> Unit,
    showIta: Boolean,
    onItaToggle: (Boolean) -> Unit,
    itaIterations: Int,
    lastProcTime: Double,
    fps: Long
) {
    val backgroundColor = when (alertColor) {
        "RED" -> Color.Red.copy(alpha = 0.5f)
        "YELLOW" -> Color.Yellow.copy(alpha = 0.5f)
        else -> Color.Green.copy(alpha = 0.5f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
        color = Color.Black.copy(alpha = 0.7f),
        contentColor = Color.White
    ) {
        LazyColumn(modifier = Modifier.padding(16.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(24.dp).background(backgroundColor))
                    Spacer(Modifier.width(12.dp))
                    Text("r1: ${"%.4f".format(r1)} | Status: $status", fontSize = 18.sp)
                }
            }
            item {
                Text("Discard Rate: ${"%.2f".format(discardRate * 100)}% | ${"%.2f".format(lastProcTime)} ms | $fps FPS", fontSize = 16.sp)
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("θ: ${"%.2f".format(theta)}", modifier = Modifier.width(60.dp))
                    Slider(
                        value = theta,
                        onValueChange = onThetaChange,
                        valueRange = 0.5f..0.99f,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isDpmActive, onCheckedChange = onDpmToggle)
                    Text("DPM (Exp 2)")
                    Spacer(Modifier.width(16.dp))
                    Checkbox(checked = showRoi, onCheckedChange = onRoiToggle)
                    Text("ROI (Exp 3)")
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = showIta, onCheckedChange = onItaToggle)
                    Text("ITA (Exp 5)")
                    if (showIta) {
                        Text(" | $itaIterations iterations", color = Color.LightGray)
                    }
                }
            }
            item {
                Text("CRE Risk: ${"%.2f".format(1.0 - r1)} | Exp 4: approach wall", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

fun ImageProxy.toGrayscaleBitmap(): Bitmap {
    val plane = planes[0]
    val buffer = plane.buffer
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride
    val width = width
    val height = height
    
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    
    val rowBuffer = ByteArray(rowStride)
    for (y in 0 until height) {
        buffer.position(y * rowStride)
        buffer.get(rowBuffer, 0, rowStride)
        for (x in 0 until width) {
            val grey = rowBuffer[x * pixelStride].toInt() and 0xFF
            pixels[y * width + x] = 0xFF000000.toInt() or (grey shl 16) or (grey shl 8) or grey
        }
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}
