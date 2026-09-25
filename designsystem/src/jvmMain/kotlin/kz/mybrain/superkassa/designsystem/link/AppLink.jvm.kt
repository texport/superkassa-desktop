package kz.mybrain.superkassa.designsystem.link

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler

/** На компьютере ссылку открывает система — браузером. */
@Composable
actual fun rememberAppLink(app: String): (String) -> Boolean {
    val links = LocalUriHandler.current
    return { link -> runCatching { links.openUri(link) }.isSuccess }
}
