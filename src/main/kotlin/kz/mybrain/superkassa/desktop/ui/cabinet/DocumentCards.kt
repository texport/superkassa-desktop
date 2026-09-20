package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetCashMovementDetails
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetReportDetails
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetShift
import kz.mybrain.superkassa.desktop.server.cabinet.ShiftTotals
import kz.mybrain.superkassa.desktop.ui.components.DetailLine
import kz.mybrain.superkassa.desktop.ui.components.MinorSumLine
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.history.JournalDelivery
import kz.mybrain.superkassa.desktop.ui.history.JournalDeliveryChip
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

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
        trailing = { CardTail(cabinetState(report.deliveryStatus, report.sendStatus), texts, onClose) }
    ) {
        DetailLine(texts.documentMoment, cabinetMoment(report.createdAt))
        DetailLine(texts.receipts, report.receiptsCount?.toString())
        DetailLine(texts.kkmDocumentNumber, report.kkmDocumentNumber)
        MinorSumLine(texts.sales, cabinetSum(report.total))
        MinorSumLine(texts.returns, cabinetSum(report.returnTotal))
        // Покупка у населения — там же, где она стоит на ленте кассы:
        // отчёт в кабинете обязан сходиться с отчётом, который кассир
        // держит в руках.
        report.buyTotal?.let { MinorSumLine(texts.operationPurchase, cabinetSum(it)) }
        report.buyReturnTotal?.let { MinorSumLine(texts.operationPurchaseReturn, cabinetSum(it)) }
        MinorSumLine(texts.cashInDrawer, cabinetSum(report.cashBalance))
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
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CabinetStatusChip(shift.state, texts)
                TextButton(onClick = onClose) { Text(texts.close) }
            }
        }
    ) {
        DetailLine(texts.openedAt, cabinetMoment(shift.openedAt))
        DetailLine(texts.closedAt, cabinetMoment(shift.closedAt))
        MinorSumLine(texts.revenue, cabinetSum(shift.total))
        ShiftTotalsLines(shift.totals, texts)
    }
}

/** Итоги смены: наличные в ящике, продажи и возвраты числом и суммой. */
@Composable
private fun ShiftTotalsLines(totals: ShiftTotals?, texts: CabinetTexts) {
    val sums = totals ?: return
    MinorSumLine(texts.cashInDrawer, cabinetSum(sums.cashSum))
    DetailLine(texts.receipts, sums.receiptsCount.toString())
    MinorSumLine(texts.sales, cabinetSum(sums.salesSum))
    MinorSumLine(texts.returns, cabinetSum(sums.returnsSum))
    // Покупка у населения показывается, только когда она в смене была:
    // у торговой точки без приёма от населения строка стояла бы нулём
    // в каждой смене и занимала место зря.
    sums.purchasesSum?.let { MinorSumLine(texts.operationPurchase, cabinetSum(it)) }
    sums.purchaseReturnsSum?.let { MinorSumLine(texts.operationPurchaseReturn, cabinetSum(it)) }
}

/** Внесение или изъятие: сумма, смена и кто оформил. */
@Composable
fun CashMovementCard(movement: CabinetCashMovementDetails, texts: CabinetTexts, onClose: () -> Unit) {
    SectionCard(
        title = documentTitle(movement.type, texts),
        trailing = { CardTail(cabinetState(null, movement.sendStatus), texts, onClose) }
    ) {
        DetailLine(texts.documentMoment, cabinetMoment(movement.createdAt))
        DetailLine(texts.shift, movement.shiftNumber?.toString())
        DetailLine(texts.documentNumber, movement.protocolDocumentId)
        MinorSumLine(documentTitle(movement.type, texts), cabinetSum(movement.amount))
    }
}

/** Правый край карточки: состояние доставки, если оно есть, и выход. */
@Composable
private fun CardTail(delivery: JournalDelivery?, texts: CabinetTexts, onClose: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        JournalDeliveryChip(delivery)
        TextButton(onClick = onClose) { Text(texts.close) }
    }
}
