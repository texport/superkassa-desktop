package kz.mybrain.superkassa.designsystem.link

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler

/** На iOS ссылку открывает система: приложение, закрепившее адрес, или браузер. */
@Composable
actual fun rememberAppLink(app: String): (String) -> Boolean {
    val links = LocalUriHandler.current
    return { link -> runCatching { links.openUri(link) }.isSuccess }
}
