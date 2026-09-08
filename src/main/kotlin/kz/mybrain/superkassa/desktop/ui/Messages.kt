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
            is Message.Refusal -> "${current.text} · ${texts.common.refusalCode}: ${current.code}"
            is Message.NodeUnavailable -> "${texts.common.nodeUnavailable} · ${current.what}: ${current.reason}"
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
