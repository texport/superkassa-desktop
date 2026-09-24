package kz.mybrain.superkassa.presentation.journal.documents.component

import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryResponse
import io.github.texport.superkassa.core.presentation.api.model.delivery.ReceiptDeliveryState
import kz.mybrain.superkassa.presentation.common.format.Dates
import kz.mybrain.superkassa.presentation.common.status.Chip
import kz.mybrain.superkassa.presentation.common.strings.LocalLanguage
import kz.mybrain.superkassa.presentation.theme.StatusColors
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs
import kz.mybrain.superkassa.presentation.words.common.of
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.journal.DeliveryTexts

/**
 * Доставка по одному каналу: куда, что с ней и почему не вышло.
 *
 * Причина отказа — словами ядра на языке кассира. Код отказа кассиру
 * не показывается: `DELIVERY_SMS_NOT_CONFIGURED` ему ничего не объясняет
 * и на узком окне рвался посреди слова; он остаётся в журнале ядра.
 * Получателя здесь нет: это данные покупателя.
 */
@Composable
internal fun DeliveryChannelRow(delivery: ReceiptDeliveryResponse, texts: DeliveryTexts) {
    val language = LocalLanguage.current
    ListItem(
        colors = ListItemDefaults.colors(containerColor = AlertDialogDefaults.containerColor),
        headlineContent = { Text(channelTitle(delivery, texts)) },
        supportingContent = { Text(deliveryDetails(delivery, texts, language)) },
        trailingContent = { Chip(stateTitle(delivery.state, texts), stateColor(delivery.state)) }
    )
}

/** Канал и вид отправки: у канала их бывает два — ссылка и сам чек. */
internal fun channelTitle(delivery: ReceiptDeliveryResponse, texts: DeliveryTexts): String {
    val channel = when (delivery.channel.uppercase()) {
        "SMS" -> texts.sms
        "TELEGRAM" -> texts.telegram
        "WHATSAPP" -> texts.whatsapp
        "EMAIL" -> texts.email
        "PRINT" -> return texts.printer
        else -> texts.otherChannel
    }
    val payload = when (delivery.payloadType.uppercase()) {
        "LINK" -> texts.link
        "PDF" -> PDF
        "IMAGE" -> texts.image
        "HTML" -> texts.page
        else -> null
    }
    return listOfNotNull(channel, payload).joinToString(Glyphs.SEPARATOR)
}

/**
 * Подробности строки: когда доставлен, сколько попыток и когда следующая,
 * причина последнего отказа.
 */
internal fun deliveryDetails(
    delivery: ReceiptDeliveryResponse,
    texts: DeliveryTexts,
    language: Language
): String {
    val reason = delivery.failureMessage?.of(language)?.takeIf { it.isNotBlank() }
    val parts = when (delivery.state) {
        ReceiptDeliveryState.DELIVERED -> listOf("${texts.delivered}: ${Dates.moment(delivery.updatedAt)}")
        else -> listOfNotNull(
            "${texts.attempts}: ${delivery.attempts}",
            delivery.nextAttemptAt?.let { "${texts.nextAttempt}: ${Dates.moment(it)}" },
            reason
        )
    }
    return parts.joinToString(Glyphs.SEPARATOR)
}

private fun stateTitle(state: ReceiptDeliveryState, texts: DeliveryTexts): String = when (state) {
    ReceiptDeliveryState.PENDING -> texts.pending
    ReceiptDeliveryState.DELIVERED -> texts.delivered
    ReceiptDeliveryState.FAILED -> texts.failed
}

@Composable
private fun stateColor(state: ReceiptDeliveryState): Color = when (state) {
    ReceiptDeliveryState.PENDING -> StatusColors.pending
    ReceiptDeliveryState.DELIVERED -> StatusColors.delivered
    ReceiptDeliveryState.FAILED -> StatusColors.refused
}

/** Название формата: одно на трёх языках. */
private const val PDF = "PDF"
