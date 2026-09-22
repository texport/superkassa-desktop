package kz.mybrain.superkassa.desktop.ui.returns

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.ui.components.FieldButton
import kz.mybrain.superkassa.desktop.ui.components.FieldButtonKind
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.components.MoneyField
import kz.mybrain.superkassa.desktop.ui.components.fieldWidth
import kz.mybrain.superkassa.desktop.ui.payment.SplitIssue
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.PaymentTexts
import kz.mybrain.superkassa.desktop.ui.strings.ReturnJournalTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Чек-основание в лицо: номер, сумма крупно, фискальный признак.
 *
 * Сумма чека — главное число панели, и набрана она тем же начертанием,
 * что итог чека на экране продажи: кассир сверяет её с бумагой в руке.
 */
@Composable
internal fun RefundSummary(basis: Document, journal: ReturnJournalTexts, total: Long) {
    val texts = LocalStrings.current
    // Номер — тот, что стоит на бумажном чеке покупателя: его касса
    // присваивает сама. Здесь стоял номер от ОФД, то есть фискальный
    // признак, и заголовок расходился со списком рядом и с бумагой.
    Text(
        text = "${journal.basis}: ${texts.returns.receiptNo} ${basis.number ?: Glyphs.DASH}",
        style = MaterialTheme.typography.titleMedium
    )
    Text(
        text = journal.receiptTotal,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(Money.formatTiyn(total), style = MoneyStyle.hero, modifier = Modifier.fillMaxWidth())
    Text(
        text = "${journal.fiscalSign}: ${basis.fiscalSign ?: basis.autonomousSign ?: Glyphs.DASH}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Сумма возврата: моноширинно и вправо, как и всякая сумма в кассе. */
@Composable
internal fun RefundAmountRow(
    journal: ReturnJournalTexts,
    entered: String,
    rejected: Boolean,
    onEnter: (String) -> Unit,
    onWholeReceipt: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.Top
    ) {
        MoneyField(
            value = entered,
            label = journal.amount,
            modifier = Modifier.fieldWidth(journal.amount, Sizes.fieldAmount),
            isError = rejected,
            onValueChange = onEnter
        )
        FieldButton(journal.wholeReceipt, FieldButtonKind.Text, onClick = onWholeReceipt)
    }
}

/** Пояснение о частичном возврате и, если есть, отказ по введённой сумме. */
@Composable
internal fun RefundHints(
    journal: ReturnJournalTexts,
    checked: RefundAmount,
    split: SplitIssue?,
    payment: PaymentTexts
) {
    Text(
        text = journal.partialHint,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val problem = when {
        checked is RefundAmount.Rejected -> problemText(checked.reason, journal)
        split == SplitIssue.Empty -> payment.splitEmpty
        split == SplitIssue.Excess -> payment.splitExcess
        else -> null
    }
    if (problem != null) {
        Text(
            text = problem,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

/**
 * Строка о том, чем обернётся набранное, — приглушённой ролью.
 *
 * Это не ошибка ввода: набрано верно, но чек уйдёт не таким, каким его
 * читает экран, или денег в ящике меньше, чем отдают покупателю. Красным
 * такие строки не красят — красное кассир читает как поломку.
 */
@Composable
internal fun RefundNote(text: String?) {
    if (text == null) return
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private fun problemText(reason: RefundProblem, journal: ReturnJournalTexts): String = when (reason) {
    RefundProblem.Empty -> journal.amountEmpty
    RefundProblem.NotANumber -> journal.amountInvalid
    RefundProblem.NotPositive -> journal.amountEmpty
    RefundProblem.TooLarge -> journal.amountTooLarge
}
