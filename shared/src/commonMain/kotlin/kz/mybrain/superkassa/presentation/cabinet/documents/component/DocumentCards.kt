package kz.mybrain.superkassa.presentation.cabinet.documents.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import kz.mybrain.superkassa.designsystem.format.Dates
import kz.mybrain.superkassa.designsystem.section.DetailLine
import kz.mybrain.superkassa.designsystem.section.MinorSumLine
import kz.mybrain.superkassa.designsystem.section.SectionCard
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetCashMovementDetails
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetReportDetails
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetShift
import kz.mybrain.superkassa.domain.cabinet.model.documents.ShiftTotals
import kz.mybrain.superkassa.presentation.cabinet.documents.cabinetState
import kz.mybrain.superkassa.presentation.cabinet.documents.documentTitle
import kz.mybrain.superkassa.presentation.common.document.JournalDelivery
import kz.mybrain.superkassa.presentation.common.document.JournalDeliveryChip
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.common.status.CabinetStatusChip
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Карточки документов, кроме чека: отчёт, смена и движение денег.
 *
 * Собраны вместе намеренно — это один и тот же разговор с владельцем:
 * что за документ, когда его принял ОФД и какие в нём числа. Чек стоит
 * отдельно: у него есть состав, оплаты и налоги, и это вдвое больше
 * разметки, чем у всех троих вместе.
 *
 * Все три построены на общих строках приложения, а не на своей вёрстке:
 * подпись и значение выглядят одинаково и здесь, и в карточке кассы,
 * и в итогах чека.
 */
@Composable
fun ReportCard(report: CabinetReportDetails, texts: CabinetTexts, onClose: () -> Unit) {
    SectionCard(
        title = listOfNotNull(
            documentTitle(report.type, texts),
            report.shiftNumber?.let { "${texts.shift} $it" }
        ).joinToString(Glyphs.SEPARATOR),
        info = texts.hints.reportCard,
        trailing = { CardTail(cabinetState(report.deliveryStatus, report.sendStatus), texts, onClose) }
    ) {
        DetailLine(texts.documentMoment, Dates.momentOf(report.createdAt))
        DetailLine(texts.receipts, report.receiptsCount?.toString())
        DetailLine(texts.kkmDocumentNumber, report.kkmDocumentNumber)
        MinorSumLine(texts.sales, Money.format(report.total))
        MinorSumLine(texts.returns, Money.format(report.returnTotal))
        // Покупка у населения — там же, где она стоит на ленте кассы:
        // отчёт в кабинете обязан сходиться с отчётом, который кассир
        // держит в руках.
        report.buyTotal?.let { MinorSumLine(texts.operationPurchase, Money.format(it)) }
        report.buyReturnTotal?.let { MinorSumLine(texts.operationPurchaseReturn, Money.format(it)) }
        MinorSumLine(texts.cashInDrawer, Money.format(report.cashBalance))
    }
}

/**
 * Смена целиком.
 *
 * Кабинет не отдаёт смену отдельной ручкой: всё, что о ней известно,
 * пришло вместе со списком. Итоги показываются только если они пришли —
 * у открытой смены их ещё нет, и нули на их месте читались бы как
 * «за смену не продано ничего».
 */
@Composable
fun ShiftCard(shift: CabinetShift, texts: CabinetTexts, onClose: () -> Unit) {
    SectionCard(
        title = "${texts.shift} ${shift.shiftNumber}",
        info = texts.hints.shiftCard,
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CabinetStatusChip(shift.state, texts)
                TextButton(onClick = onClose) { Text(texts.close) }
            }
        }
    ) {
        DetailLine(texts.openedAt, Dates.momentOf(shift.openedAt))
        DetailLine(texts.closedAt, Dates.momentOf(shift.closedAt))
        MinorSumLine(texts.revenue, Money.format(shift.total))
        ShiftTotalsLines(shift.totals, texts)
    }
}

/** Итоги смены: наличные в ящике, продажи и возвраты числом и суммой. */
@Composable
private fun ShiftTotalsLines(totals: ShiftTotals?, texts: CabinetTexts) {
    val sums = totals ?: return
    MinorSumLine(texts.cashInDrawer, Money.format(sums.cashSum))
    DetailLine(texts.receipts, sums.receiptsCount.toString())
    MinorSumLine(texts.sales, Money.format(sums.salesSum))
    MinorSumLine(texts.returns, Money.format(sums.returnsSum))
    // Покупка у населения показывается, только когда она в смене была:
    // у торговой точки без приёма от населения строка стояла бы нулём
    // в каждой смене и занимала место зря.
    sums.purchasesSum?.let { MinorSumLine(texts.operationPurchase, Money.format(it)) }
    sums.purchaseReturnsSum?.let { MinorSumLine(texts.operationPurchaseReturn, Money.format(it)) }
}

/** Внесение или изъятие: сумма, смена и кто оформил. */
@Composable
fun CashMovementCard(movement: CabinetCashMovementDetails, texts: CabinetTexts, onClose: () -> Unit) {
    SectionCard(
        title = documentTitle(movement.type, texts),
        info = texts.hints.cashMovement,
        trailing = { CardTail(cabinetState(null, movement.sendStatus), texts, onClose) }
    ) {
        DetailLine(texts.documentMoment, Dates.momentOf(movement.createdAt))
        DetailLine(texts.shift, movement.shiftNumber?.toString())
        DetailLine(texts.documentNumber, movement.protocolDocumentId)
        MinorSumLine(documentTitle(movement.type, texts), Money.format(movement.amount))
    }
}

/** Правый край карточки: состояние доставки, если оно есть, и выход. */
@Composable
private fun CardTail(delivery: JournalDelivery?, texts: CabinetTexts, onClose: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        JournalDeliveryChip(delivery)
        TextButton(onClick = onClose) { Text(texts.close) }
    }
}
