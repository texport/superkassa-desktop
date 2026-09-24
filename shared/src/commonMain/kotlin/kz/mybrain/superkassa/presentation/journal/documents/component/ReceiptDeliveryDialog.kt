package kz.mybrain.superkassa.presentation.journal.documents.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import kz.mybrain.superkassa.presentation.common.dialog.DialogBody
import kz.mybrain.superkassa.presentation.common.dialog.DialogTitle
import kz.mybrain.superkassa.presentation.common.keyboard.CloseOnEscape
import kz.mybrain.superkassa.presentation.common.state.EmptyState
import kz.mybrain.superkassa.presentation.common.state.LoadingState
import kz.mybrain.superkassa.presentation.common.strings.LocalLanguage
import kz.mybrain.superkassa.presentation.journal.documents.JournalActions
import kz.mybrain.superkassa.presentation.journal.documents.ReceiptDeliveryUi
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import kz.mybrain.superkassa.strings.api.journal.DeliveryTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Доставка чека покупателю — по каналу на строку.
 *
 * Окно, а не столбец журнала: доставка читается у кассы по одному чеку,
 * и спрашивать её для тысяч строк срока ради столбца незачем. Кассир
 * открывает чек, когда покупатель говорит «мне ничего не пришло».
 *
 * Главное действие — «Отправить ещё раз» — стоит, только когда есть что
 * повторять: доставка хотя бы по одному каналу окончательно не удалась.
 * Ждущее касса дошлёт сама, и кнопка над ним обещала бы то, чего не будет.
 */
@Composable
fun ReceiptDeliveryDialog(delivery: ReceiptDeliveryUi, actions: JournalActions) {
    val texts = textsOf(LocalLanguage.current).journal.delivery
    CloseOnEscape { actions.closeDelivery() }
    AlertDialog(
        onDismissRequest = actions::closeDelivery,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.widthIn(max = Sizes.formDialog).fillMaxWidth(),
        icon = { Icon(AppIcons.receiptDelivery, contentDescription = null) },
        // Знак номера не отрывается от числа при переносе заголовка.
        title = { DialogTitle("${texts.title} ${Glyphs.NUMBER}${Glyphs.NBSP}${delivery.number}") },
        text = { DialogBody { DeliveryBody(delivery, texts) } },
        confirmButton = {
            if (delivery.resendable) {
                Button(enabled = delivery.canResend, onClick = actions::resend) { Text(texts.resend) }
            } else {
                TextButton(onClick = actions::closeDelivery) { Text(texts.close) }
            }
        },
        dismissButton = {
            if (delivery.resendable) TextButton(onClick = actions::closeDelivery) { Text(texts.close) }
        }
    )
}

/** Что стоит в окне: ожидание, отказ кассы, объяснение пустоты или каналы. */
@Composable
private fun DeliveryBody(delivery: ReceiptDeliveryUi, texts: DeliveryTexts) {
    delivery.problem?.let { problem ->
        Text(problem, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
    when {
        delivery.reading -> LoadingState(dense = true)
        delivery.deliveries.isNotEmpty() -> delivery.deliveries.forEach { DeliveryChannelRow(it, texts) }
        delivery.problem != null -> Unit
        delivery.awaitingBfd -> NoDelivery(texts.awaitingBfd, texts.awaitingBfdHint)
        else -> NoDelivery(texts.notOrdered, texts.notOrderedHint)
    }
}

/** Доставки нет — и почему: значок, строка и подсказка. */
@Composable
private fun NoDelivery(title: String, hint: String) {
    EmptyState(AppIcons.receiptDelivery, title, hint, dense = true)
}
