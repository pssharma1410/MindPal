package com.psgcreations.mindjournalai.ui.ocr

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// --- CHANGED: Added parameters to receive the callbacks ---
@Composable
fun OCRMainScreen(
    onTextAccepted: (String) -> Unit,
    onBack: () -> Unit
) {
    var recognizedText by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(true) }
    var hasPermission by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (!hasPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Camera permission required.")
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) {
                    Text("Go Back")
                }
            }
        }
    } else {
        if (isScanning) {
            OCRCameraScreen(
                onTextRecognized = { result ->
                    recognizedText = result
                    isScanning = false
                },
                onError = { /* Handle error */ }
            )
        } else {
            // Show Result
            OCRResultView(
                text = recognizedText,
                onRetake = {
                    recognizedText = ""
                    isScanning = true
                },
                onUseText = {
                    // --- CHANGED: Call the callback when user accepts text ---
                    onTextAccepted(recognizedText)
                }
            )
        }
    }
}

@Composable
fun OCRResultView(
    text: String,
    onRetake: () -> Unit,
    onUseText: () -> Unit // --- CHANGED: Added this parameter ---
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Scanned Text",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                if (text.isBlank()) {
                    Text("No text found.", style = MaterialTheme.typography.bodyLarge)
                } else {
                    Text(text, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.weight(1f)
            ) {
                Text("Retake")
            }

            Button(
                onClick = onUseText,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Use Text")
            }
        }
    }
}