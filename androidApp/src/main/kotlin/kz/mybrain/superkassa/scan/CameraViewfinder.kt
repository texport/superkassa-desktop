package kz.mybrain.superkassa.scan

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Картинка задней камеры и распознавание кодов в фоне.
 *
 * Камера привязана к жизни экрана: свернули кассу — камера отпущена,
 * закрыли сканер — тоже. Кадры разбирает один поток анализа CameraX;
 * пока идёт разбор, новые кадры не копятся — берётся последний.
 */
@Composable
internal fun CameraViewfinder(onCode: (String) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val found by rememberUpdatedState(onCode)
    val analysis = remember { Executors.newSingleThreadExecutor() }
    val cameras = remember { ProcessCameraProvider.getInstance(context) }
    DisposableEffect(Unit) {
        onDispose {
            if (cameras.isDone) cameras.get().unbindAll()
            analysis.shutdown()
        }
    }
    AndroidView(
        factory = { viewContext ->
            PreviewView(viewContext).also { view ->
                val main = ContextCompat.getMainExecutor(viewContext)
                val analyzer = CodeAnalyzer { code -> main.execute { found(code) } }
                cameras.addListener({ bind(cameras.get(), owner, view, analyzer, analysis) }, main)
            }
        },
        modifier = modifier
    )
}

/** Картинка — в видоискатель, кадры — разборщику, задняя камера — к жизни экрана. */
private fun bind(
    cameras: ProcessCameraProvider,
    owner: LifecycleOwner,
    view: PreviewView,
    analyzer: CodeAnalyzer,
    analysis: Executor
) {
    val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
    val frames = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also { it.setAnalyzer(analysis, analyzer) }
    cameras.unbindAll()
    cameras.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, frames)
}
