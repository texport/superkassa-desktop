package kz.mybrain.superkassa.presentation.common.message

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
import kz.mybrain.superkassa.designsystem.adaptive.LocalWindowClass
import kz.mybrain.superkassa.designsystem.adaptive.WidthClass
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.strings.api.common.AppStrings

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
 * Снекбар встаёт внизу окна по центру, как велит Material 3, и не шире
 * своего предела. Прежде он стоял у левого края над нижней полосой входа,
 * то есть посреди экрана слева, и закрывал двери «Новая касса» и «Кабинет
 * БФД». Экран входа оставляет под ним запас у нижнего края.
 *
 * В окне уже большого класса ([WidthClass.Large]) по центру он ложится на
 * кассу продажи — на «Пробить чек» и «Принято», — и кассир не может нажать
 * кнопку, пока читает, почему её нажатие не прошло. Там снекбар встаёт
 * у начального края: касса стоит справа, а слева он закрывает рельс
 * и край чека, а не действие.
 */
@Composable
fun MessageHost(state: SnackbarHostState) {
    val centered = LocalWindowClass.current.width >= WidthClass.Large
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (centered) Alignment.BottomCenter else Alignment.BottomStart
    ) {
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
