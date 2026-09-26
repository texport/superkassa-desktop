package kz.mybrain.superkassa.presentation.common.print

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.domain.print.model.ShareWay
import kz.mybrain.superkassa.strings.api.common.ShareTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * «Поделиться» и выбор пути, когда путей несколько.
 *
 * Путь один — окно «Поделиться» Android — кнопка открывает его сразу.
 * На компьютере путей три, и кнопка раскрывает меню Material 3 (Menus):
 * WhatsApp, Telegram, почта. Вид кнопки — у того, кто её ставит: в шапке
 * формы она текстовая, под итогом чека — обводная.
 *
 * @param ways пути этой машины; пусто — кнопки нет.
 * @param button кнопка «Поделиться» с тем, что сделать по нажатию.
 */
@Composable
fun ShareAction(
    ways: List<ShareWay>,
    onShare: (ShareWay) -> Unit,
    button: @Composable (onClick: () -> Unit) -> Unit
) {
    if (ways.isEmpty()) return
    val texts = textsOf(LocalLanguage.current).common.share
    var open by remember { mutableStateOf(false) }
    Box {
        button { if (ways.size == 1) onShare(ways.single()) else open = true }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            ways.forEach { way ->
                DropdownMenuItem(
                    text = { Text(way.title(texts)) },
                    onClick = {
                        open = false
                        onShare(way)
                    }
                )
            }
        }
    }
}

/** Название пути в меню. */
private fun ShareWay.title(texts: ShareTexts): String = when (this) {
    ShareWay.System -> texts.share
    ShareWay.WhatsApp -> texts.whatsApp
    ShareWay.Telegram -> texts.telegram
    ShareWay.Email -> texts.email
}
