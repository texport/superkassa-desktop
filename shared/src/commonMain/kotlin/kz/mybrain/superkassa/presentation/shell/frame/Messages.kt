package kz.mybrain.superkassa.presentation.shell.frame

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.strings.common.AppStrings
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs

/**
 * Сообщения кассиру — снекбаром поверх содержимого.
 *
 * Раньше это была полоса над экраном, и появление сообщения сдвигало
 * вниз всё под ним: кассир целился в поле цены, а попадал в наименование,
 * потому что разметка успевала уехать между взглядом и нажатием. Снекбар
 * по Material 3 лежит поверх и ничего не двигает.
 *
 * Удача гаснет быстро, отказ держится дольше: «чек пробит» кассир видит
 * краем глаза, а причину отказа читает и решает, что делать. Гаснут оба:
 * снекбар лежит поверх содержимого, и оставленный навсегда отказ закрывал
 * нижний край экрана до тех пор, пока его не заметят и не нажмут «Скрыть».
 *
 * Снекбар встаёт у левого края окна, как велит Material 3 для больших
 * экранов, а не посередине. Посередине он ложился на кассу продажи —
 * на «Пробить чек» и «Принято», — и кассир не мог нажать кнопку, пока
 * читал, почему её нажатие не прошло. Касса стоит справа, и слева
 * снекбар закрывает рельс и край чека, а не действие.
 */
@Composable
fun MessageHost(state: SnackbarHostState) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomStart) {
        SnackbarHost(hostState = state) { data ->
            Snackbar(snackbarData = data)
        }
    }
}

/**
 * Показывает сообщение окна и снимает его, когда снекбар закрыт.
 *
 * @param message что случилось; `null` — ничего не показывать.
 * @param state состояние снекбара каркаса.
 * @param onDismiss снять сообщение из строки сообщений окна.
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
        // Сообщения нет — значит причина устранена, и висящей строке
        // на экране больше не место. Прежде снекбар оставался стоять:
        // отказ показывается до тех пор, пока его не закроют, и владелец
        // читал «Кабинет не отвечает» под четвёртым успешным шагом мастера.
        val current = shown ?: run {
            state.currentSnackbarData?.dismiss()
            return@LaunchedEffect
        }
        val text = messageText(current, texts)
        val result = state.showSnackbar(
            message = text,
            actionLabel = texts.common.hide,
            withDismissAction = current !is Message.Done,
            duration = durationOf(current)
        )
        if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) onDismiss()
    }
}

/**
 * Сообщение одной строкой снекбара.
 *
 * Код отказа на экране кассиру ничего не даёт: касса отвечает словами,
 * и «KKM_BRANDING_SETTINGS_REQUIRES_PROGRAMMING» рядом с ними — служебный
 * шум. Код уходит в журнал, а на экран попадает только там, где слов нет вовсе.
 */
internal fun messageText(message: Message, texts: AppStrings): String = when (message) {
    is Message.Done -> message.text
    is Message.Refusal -> message.text.ifBlank { "${texts.common.refusalCode}: ${message.code}" }
    is Message.NoAnswer -> "${texts.common.noAnswer}${Glyphs.SEPARATOR}${message.what}"
    is Message.Failed -> "${texts.common.kassaFailed}${Glyphs.SEPARATOR}${message.what}"
}

/**
 * Сколько сообщение стоит на экране.
 *
 * Отказ стоял, пока его не закроют. Кассир нажимает «Скрыть» не сразу
 * и не всегда: снекбар лежит поверх содержимого, и оставшийся отказ
 * закрывал нижний край экрана — кнопку оплаты, последнюю строку чека, —
 * а через минуту говорил уже не о том, что кассир делает сейчас. Отказ
 * гаснет сам, но заметно дольше удачи: причину надо успеть прочитать.
 * «Скрыть» остаётся на месте для тех, кто прочитал раньше.
 */
internal fun durationOf(message: Message): SnackbarDuration =
    if (message is Message.Done) SnackbarDuration.Short else SnackbarDuration.Long
