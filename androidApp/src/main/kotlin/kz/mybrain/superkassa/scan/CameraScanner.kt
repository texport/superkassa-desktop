package kz.mybrain.superkassa.scan

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import kz.mybrain.superkassa.presentation.common.scan.CameraAccess
import kz.mybrain.superkassa.presentation.common.scan.CodeCamera

/**
 * Камера Android как сканер штрихкодов: CameraX и распознавание ZXing.
 *
 * ZXing — чистая Java без сервисов Google Play: касса работает и на Huawei
 * без них. Разрешение на камеру спрашивается по месту, когда кассир открыл
 * сканер, а не при запуске: без сканера касса камеры не касается.
 */
class CameraScanner(private val context: Context) : CodeCamera {

    override val available: Boolean = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

    @Composable
    override fun access(): CameraAccess {
        val activity = LocalActivity.current
        var granted by remember { mutableStateOf(granted()) }
        var refused by remember { mutableStateOf(false) }
        val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
            granted = ok
            refused = !ok
        }
        // Отказ «больше не спрашивать» система не говорит прямо: после отказа
        // она перестаёт просить объяснить — значит, спросить уже нельзя.
        val blocked = refused && activity != null &&
            !ActivityCompat.shouldShowRequestPermissionRationale(activity, CAMERA)
        return when {
            granted -> CameraAccess.Granted
            blocked -> CameraAccess.Denied(::openSettings)
            else -> CameraAccess.Missing { ask.launch(CAMERA) }
        }
    }

    @Composable
    override fun Viewfinder(onCode: (String) -> Unit, modifier: Modifier) = CameraViewfinder(onCode, modifier)

    private fun granted(): Boolean =
        ContextCompat.checkSelfPermission(context, CAMERA) == PackageManager.PERMISSION_GRANTED

    /** Настройки приложения в системе: там включают отказанное разрешение. */
    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private companion object {
        const val CAMERA = Manifest.permission.CAMERA
    }
}
