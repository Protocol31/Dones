package com.dones.app.qr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dones.app.ui.BackLink
import com.dones.app.ui.Body
import com.dones.app.ui.PrimaryButton
import com.dones.app.ui.Screen
import com.dones.app.ui.Title
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

// W1b: leer el QR de la pantalla del maestro. Funciona sin internet (ML Kit en el teléfono).
@Composable
fun QrScannerScreen(onResult: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(hasCameraPermission(context)) }
    var asked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
        asked = true
    }
    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(Manifest.permission.CAMERA) }

    Screen(bottom = { BackLink(onClick = onBack) }) {
        Title("Apunta a la pantalla del maestro")
        if (hasPermission) {
            CameraPreview(lifecycleOwner, onResult, Modifier.fillMaxWidth().aspectRatio(3f / 4f))
            Body("Se conecta solo al leer el código.")
        } else {
            Body("Para leer el código QR necesitamos usar la cámara. Solo se usa en esta pantalla.")
            if (asked) {
                Body("Si no quieres activarla, vuelve y escribe el código de 6 números.")
            }
            PrimaryButton("Permitir la cámara", onClick = { launcher.launch(Manifest.permission.CAMERA) })
        }
    }
}

private fun hasCameraPermission(context: Context) =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

@Composable
private fun CameraPreview(lifecycleOwner: LifecycleOwner, onResult: (String) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val providerFuture = remember { ProcessCameraProvider.getInstance(context) }
    DisposableEffect(Unit) {
        onDispose { runCatching { providerFuture.get().unbindAll() } }
    }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PreviewView(ctx).also { previewView ->
                providerFuture.addListener(
                    { bindCamera(ctx, providerFuture.get(), lifecycleOwner, previewView, onResult) },
                    ContextCompat.getMainExecutor(ctx),
                )
            }
        },
    )
}

@OptIn(ExperimentalGetImage::class)
private fun bindCamera(
    context: Context,
    provider: ProcessCameraProvider,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    onResult: (String) -> Unit,
) {
    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
    val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    )
    var done = false
    val analysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
    analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { proxy ->
        val media = proxy.image
        if (media == null || done) {
            proxy.close()
            return@setAnalyzer
        }
        val input = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { codes ->
                val value = codes.firstNotNullOfOrNull { it.rawValue }
                if (value != null && !done) {
                    done = true
                    onResult(value)
                }
            }
            .addOnCompleteListener { proxy.close() }
    }
    provider.unbindAll()
    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
}
