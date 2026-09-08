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
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
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
        ).joinToString(" · "),
        trailing = { CardTail(report.deliveryStatus, texts, onClose) }
    ) {
        DetailLine(texts.documentMoment, cabinetMoment(report.createdAt))
        DetailLine(texts.receiptMoment, cabinetMoment(report.kkmTime))
        DetailLine(texts.openedAt, cabinetMoment(report.shiftOpenedAt))
        DetailLine(texts.closedAt, cabinetMoment(report.shiftClosedAt))
        DetailLine(texts.registrationNumber, report.registrationNumber)
        MinorSumLine(texts.revenue, cabinetSum(report.total))
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
    MinorSumLine("${texts.sales} · ${sums.salesCount}", cabinetSum(sums.salesSum))
    MinorSumLine("${texts.returns} · ${sums.returnsCount}", cabinetSum(sums.returnsSum))
}

/** Внесение или изъятие: сумма, смена и кто оформил. */
@Composable
fun CashMovementCard(movement: CabinetCashMovementDetails, texts: CabinetTexts, onClose: () -> Unit) {
    SectionCard(
        title = documentTitle(movement.type, texts),
        trailing = { CardTail(delivery = null, texts = texts, onClose = onClose) }
    ) {
        DetailLine(texts.documentMoment, cabinetMoment(movement.createdAt))
        DetailLine(texts.receiptMoment, cabinetMoment(movement.kkmTime))
        DetailLine(texts.shift, movement.shiftNumber?.toString())
        DetailLine(texts.operator, movement.operator?.name)
        DetailLine(texts.registrationNumber, movement.registrationNumber)
        if (movement.offline == true) {
            DetailLine(texts.status, texts.autonomous)
        }
        MinorSumLine(documentTitle(movement.type, texts), cabinetSum(movement.amount))
    }
}

/** Правый край карточки: состояние доставки, если оно есть, и выход. */
@Composable
private fun CardTail(delivery: String?, texts: CabinetTexts, onClose: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (delivery != null) DeliveryChip(delivery, texts)
        TextButton(onClick = onClose) { Text(texts.close) }
    }
}
