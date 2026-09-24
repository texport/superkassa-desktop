package kz.mybrain.superkassa.presentation.cabinet.documents.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import kz.mybrain.superkassa.designsystem.format.Dates
import kz.mybrain.superkassa.designsystem.section.DetailLine
import kz.mybrain.superkassa.designsystem.section.SectionCard
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetReceiptDetails
import kz.mybrain.superkassa.presentation.cabinet.documents.cabinetState
import kz.mybrain.superkassa.presentation.cabinet.documents.documentTitle
import kz.mybrain.superkassa.presentation.common.document.JournalDeliveryChip
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

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
        DetailLine(texts.receiptMoment, Dates.momentOf(receipt.createdAt))
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
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        JournalDeliveryChip(cabinetState(receipt.deliveryStatus, receipt.sendStatus))
        TextButton(onClick = onClose) { Text(texts.close) }
    }
}
