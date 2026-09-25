package kz.mybrain.superkassa.designsystem.link

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Сперва — в самом приложении [app], затем — всякому, кто откроет ссылку.
 *
 * Видеть приложение по имени пакета на Android 11+ разрешает запись
 * `<queries>` в манифесте приложения; без неё первая попытка не находит
 * приложения и ссылка уходит в браузер.
 */
@Composable
actual fun rememberAppLink(app: String): (String) -> Boolean {
    val context = LocalContext.current
    return remember(context, app) { { link -> opened(context, app, link) } }
}

private fun opened(context: Context, app: String, link: String): Boolean {
    val view = Intent(Intent.ACTION_VIEW, Uri.parse(link))
    return started(context, Intent(view).setPackage(app)) || started(context, view)
}

private fun started(context: Context, intent: Intent): Boolean = try {
    context.startActivity(intent)
    true
} catch (_: ActivityNotFoundException) {
    false
}
