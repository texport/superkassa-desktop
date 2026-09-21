package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetReceiptDetails
import kz.mybrain.superkassa.desktop.ui.components.DetailLine
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.history.JournalDeliveryChip
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Чек целиком, как его принял ОФД.
 *
 * Список показывает строку, а разбираться приходится с составом: сошлись
 * ли позиции с итогом, какой налог начислен, кто оформил и дошла ли
 * отметка КГД.
 *
 * Плашка состояния показывает настоящую доставку. Прежде она строилась
 * так, что любой ответ кабинета — в том числе отказ — превращался в
 * «Принято» зелёным: у чека, до ОФД не доехавшего, стояла отметка
 * о приёме.
 */
@Composable
fun ReceiptCard(receipt: CabinetReceiptDetails, texts: CabinetTexts, onClose: () -> Unit) {
    SectionCard(
        title = listOfNotNull(
            documentTitle(receipt.operationType, texts),
            receipt.receiptNumber
        ).joinToString(Glyphs.SEPARATOR),
        info = texts.hints.receiptCard,
        trailing = { ReceiptTail(receipt, texts, onClose) }
    ) {
        DetailLine(texts.receiptMoment, cabinetMoment(receipt.createdAt))
        DetailLine(texts.operator, receipt.operator?.name)
        DetailLine(texts.shift, receipt.shiftNumber?.toString())
        // Фискальный признак стоит в заголовке карточки; здесь — номер документа по счётчику кассы
        DetailLine(texts.kkmDocumentNumber, receipt.kkmDocumentNumber)
        // Отметка КГД — то, ради чего чек и смотрят в кабинете: её
        // отсутствие названо словами, а не пропущенной строкой. Слова
        // продолжают подпись, а не повторяют её: в строке стояло
        // «Отметка КГД · Отметки КГД нет».
        DetailLine(texts.kgdMarked, receipt.kgdMark ?: texts.noKgdMark)
        ReceiptBreakdown(receipt, texts)
    }
}

/** Состояние доставки и выход из карточки. */
@Composable
private fun ReceiptTail(receipt: CabinetReceiptDetails, texts: CabinetTexts, onClose: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        JournalDeliveryChip(cabinetState(receipt.deliveryStatus, receipt.sendStatus))
        TextButton(onClick = onClose) { Text(texts.close) }
    }
}
