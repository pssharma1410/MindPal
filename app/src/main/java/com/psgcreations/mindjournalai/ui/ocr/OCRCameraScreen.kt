package com.psgcreations.mindjournalai.ui.ocr

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun OCRCameraScreen(
    onTextRecognized: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // 1. Initialize the Camera Controller
    val cameraController = remember {
        LifecycleCameraController(context).apply {
            bindToLifecycle(lifecycleOwner)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    // 2. Create a background executor for image processing
    val executor = remember { Executors.newSingleThreadExecutor() }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- Camera Preview ---
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    controller = cameraController
                }
            },
            onRelease = {
                cameraController.unbind()
            }
        )

        // --- Capture Button ---
        FloatingActionButton(
            onClick = {
                captureAndProcessImage(
                    context = context,
                    controller = cameraController,
                    executor = executor,
                    onTextFound = onTextRecognized,
                    onError = { e -> onError(e.localizedMessage ?: "Unknown error") }
                )
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = Icons.Default.PhotoCamera,
                contentDescription = "Capture Text",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

/**
 * Captures the image in memory (no file saved) and passes it to ML Kit.
 */
private fun captureAndProcessImage(
    context: Context,
    controller: LifecycleCameraController,
    executor: ExecutorService,
    onTextFound: (String) -> Unit,
    onError: (Exception) -> Unit
) {
    controller.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                try {
                    // 1. Convert ImageProxy to ML Kit InputImage
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    val mediaImage = imageProxy.image

                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)

                        // 2. Process with ML Kit
                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                        recognizer.process(image)
                            .addOnSuccessListener { visionText ->
                                // Switch to Main Thread for UI
                                ContextCompat.getMainExecutor(context).execute {
                                    onTextFound(visionText.text)
                                }
                            }
                            .addOnFailureListener { e ->
                                ContextCompat.getMainExecutor(context).execute {
                                    onError(e)
                                }
                            }
                            .addOnCompleteListener {
                                // IMPORTANT: Close imageProxy to free up camera buffer
                                imageProxy.close()
                            }
                    } else {
                        // If mediaImage is null, we still must close the proxy
                        imageProxy.close()
                    }
                } catch (e: Exception) {
                    imageProxy.close()
                    ContextCompat.getMainExecutor(context).execute { onError(e) }
                }
            }

            override fun onError(exception: ImageCaptureException) {
                ContextCompat.getMainExecutor(context).execute { onError(exception) }
            }
        }
    )
}