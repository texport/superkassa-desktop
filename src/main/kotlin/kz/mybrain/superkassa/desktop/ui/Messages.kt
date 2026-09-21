package kz.mybrain.superkassa.desktop.ui

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kz.mybrain.superkassa.desktop.app.Message
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs

/**
 * Сообщения кассиру — снекбаром поверх содержимого.
 *
 * Раньше это была полоса над экраном, и появление сообщения сдвигало
 * вниз всё под ним: кассир целился в поле цены, а попадал в наименование,
 * потому что разметка успевала уехать между взглядом и нажатием. Снекбар
 * по Material 3 лежит поверх и ничего не двигает.
 *
 * Удача гаснет сама, отказ остаётся до нажатия: «чек пробит» кассир видит
 * краем глаза, а причину отказа читает и решает, что делать.
 */
@Composable
fun MessageHost(state: SnackbarHostState) {
    SnackbarHost(hostState = state) { data ->
        Snackbar(snackbarData = data)
    }
}

/**
 * Показывает сообщение сеанса и снимает его, когда снекбар закрыт.
 *
 * @param message что случилось; `null` — ничего не показывать.
 * @param state состояние снекбара каркаса.
 * @param onDismiss снять сообщение с сеанса.
 */
@Composable
fun MessageEffect(
    message: Message?,
    state: SnackbarHostState,
    onDismiss: () -> Unit
) {
    val texts = LocalStrings.current
    val shown = remember(message) { message }
    LaunchedEffect(shown) {
        val current = shown ?: return@LaunchedEffect
        val text = when (current) {
            is Message.Done -> current.text
            // Код отказа на экране кассиру ничего не даёт: узел отвечает
            // словами, и «KKM_BRANDING_SETTINGS_REQUIRES_PROGRAMMING» рядом
            // с ними — служебный шум. Код уходит в журнал, а на экран
            // попадает только там, где слов нет вовсе.
            is Message.Refusal ->
                current.text.ifBlank { "${texts.common.refusalCode}: ${current.code}" }
            is Message.NodeUnavailable -> "${texts.common.nodeUnavailable}${Glyphs.SEPARATOR}${current.what}"
            is Message.NoAnswer -> "${texts.common.nodeNoAnswer}${Glyphs.SEPARATOR}${current.what}"
        }
        val result = state.showSnackbar(
            message = text,
            actionLabel = texts.common.hide,
            withDismissAction = current !is Message.Done,
            duration = if (current is Message.Done) SnackbarDuration.Short else SnackbarDuration.Indefinite
        )
        if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) onDismiss()
    }
}
