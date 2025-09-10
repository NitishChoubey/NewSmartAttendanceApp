package com.ebf.smartattendanceapp.CameraView

import android.annotation.SuppressLint
import android.util.Log
import android.view.Surface
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "CameraView"

@Composable
fun CameraView(
    isScanningActive: Boolean,
    onQrScanned: (String?) -> Unit,
    lifecycleOwner: LifecycleOwner
) {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val onQrScannedState = rememberUpdatedState(onQrScanned)
    val isActiveState = rememberUpdatedState(isScanningActive)

    // Prevent duplicate callbacks per detection burst
    val firedOnce = remember { AtomicBoolean(false) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            }

            val cameraProvider = cameraProviderFuture.get()

            val safeRotation = try {
                previewView.display?.rotation ?: Surface.ROTATION_0
            } catch (t: Throwable) {
                Surface.ROTATION_0
            }

            val preview = Preview.Builder()
                .setTargetRotation(safeRotation)
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
                .build()
            val scanner = BarcodeScanning.getClient(options)

            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()

            var frames = 0

            @SuppressLint("UnsafeOptInUsageError")
            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                frames++
                if (frames % 50 == 0) Log.d(TAG, "frames=$frames isActive=${isActiveState.value}")

                val mediaImage = imageProxy.image
                if (mediaImage == null) {
                    imageProxy.close(); return@setAnalyzer
                }

                if (!isActiveState.value) {
                    // Drain frames while paused
                    if (frames % 50 == 0) Log.d(TAG, "paused → draining frames")
                    firedOnce.set(false) // allow next detection when resumed
                    imageProxy.close(); return@setAnalyzer
                }

                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        val value = barcodes.firstOrNull()?.rawValue
                        if (!value.isNullOrBlank()) {
                            if (firedOnce.compareAndSet(false, true)) {
                                Log.d(TAG, "QR detected: ${value.take(200)}")
                                onQrScannedState.value(value)
                            } else {
                                // Already fired for this burst; ignore until paused/resumed
                                Log.d(TAG, "QR already fired; ignoring duplicate")
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Scan failed: ${e.message}", e)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
                Log.d(TAG, "Camera bound (rotation=$safeRotation)")
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
            }

            previewView
        },
        update = {
            Log.d(TAG, "Recompose: isScanningActive=$isScanningActive")
            if (!isScanningActive) {
                // Allow next detection when scanning resumes
                firedOnce.set(false)
            }
        }
    )
}
