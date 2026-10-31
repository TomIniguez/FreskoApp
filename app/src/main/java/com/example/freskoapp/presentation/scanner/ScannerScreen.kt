package com.example.freskoapp.presentation.scanner

import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.TorchState
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.freskoapp.R
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

// Not unit-tested: CameraX and ML Kit need a real camera. Delivers at most one code, then the caller leaves.
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@OptIn(ExperimentalGetImage::class)
@Composable
fun ScannerScreen(
    onScanned: (String) -> Unit,
    onCameraUnavailable: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { LifecycleCameraController(context) }
    val currentOnScanned by rememberUpdatedState(onScanned)
    val currentOnUnavailable by rememberUpdatedState(onCameraUnavailable)
    var hasFlash by remember { mutableStateOf(false) }
    var torchOn by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val delivered = AtomicBoolean(false)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val analysisExecutor = Executors.newSingleThreadExecutor()
        // Only retail product codes; anything else is simply never reported and scanning goes on.
        val scanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
                .build()
        )
        controller.setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
        controller.setImageAnalysisAnalyzer(analysisExecutor) { imageProxy ->
            val image = imageProxy.image
            if (delivered.get() || image == null) {
                imageProxy.close()
                return@setImageAnalysisAnalyzer
            }
            scanner.process(InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees))
                .addOnSuccessListener(mainExecutor) { barcodes ->
                    val code = barcodes.firstNotNullOfOrNull { it.rawValue } ?: return@addOnSuccessListener
                    // Frames already in flight may also find a code; only the first one gets through.
                    if (delivered.compareAndSet(false, true)) {
                        controller.clearImageAnalysisAnalyzer()
                        currentOnScanned(code)
                    }
                }
                .addOnCompleteListener(mainExecutor) { imageProxy.close() }
        }
        val torchObserver = Observer<Int> { torchOn = it == TorchState.ON }
        controller.torchState.observe(lifecycleOwner, torchObserver)
        controller.initializationFuture.addListener({
            val ready = runCatching { controller.initializationFuture.get() }.isSuccess
            val back = ready && controller.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
            val front = ready && controller.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
            if (!back && !front) {
                currentOnUnavailable()
                return@addListener
            }
            // Devices with only a front camera can still scan.
            if (!back) controller.cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            hasFlash = controller.cameraInfo?.hasFlashUnit() == true
        }, mainExecutor)
        controller.bindToLifecycle(lifecycleOwner)

        onDispose {
            controller.clearImageAnalysisAnalyzer()
            controller.torchState.removeObserver(torchObserver)
            controller.unbind()
            scanner.close()
            analysisExecutor.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scanner_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back_cd))
                    }
                },
                actions = {
                    if (hasFlash) {
                        IconToggleButton(checked = torchOn, onCheckedChange = { controller.enableTorch(it) }) {
                            Icon(
                                painterResource(R.drawable.ic_flashlight),
                                contentDescription = stringResource(if (torchOn) R.string.torch_off_cd else R.string.torch_on_cd),
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding).fillMaxSize()) {
            AndroidView(
                factory = { PreviewView(it).apply { this.controller = controller } },
                modifier = Modifier.fillMaxSize(),
            )
            Text(
                stringResource(R.string.scanner_hint),
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
            )
        }
    }
}
