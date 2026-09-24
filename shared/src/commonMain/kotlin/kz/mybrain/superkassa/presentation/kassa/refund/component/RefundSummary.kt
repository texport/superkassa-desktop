package kz.mybrain.superkassa.presentation.kassa.refund.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import kz.mybrain.superkassa.domain.document.model.number
import kz.mybrain.superkassa.domain.kassa.model.payment.SplitIssue
import kz.mybrain.superkassa.domain.kassa.model.refund.RefundAmount
import kz.mybrain.superkassa.domain.kassa.model.refund.RefundDraft
import kz.mybrain.superkassa.domain.kassa.model.refund.RefundProblem
import kz.mybrain.superkassa.presentation.common.adaptive.WrapRow
import kz.mybrain.superkassa.presentation.common.button.FieldButton
import kz.mybrain.superkassa.presentation.common.button.FieldButtonKind
import kz.mybrain.superkassa.presentation.common.field.MoneyField
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.common.keyboard.SystemBack
import kz.mybrain.superkassa.presentation.common.text.MoneyText
import kz.mybrain.superkassa.presentation.kassa.refund.RefundActions
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.journal.ReturnJournalTexts
import kz.mybrain.superkassa.presentation.strings.kassa.PaymentTexts
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.presentation.theme.type.MoneyStyle

/**
 * Чек-основание в лицо: номер, сумма крупно, фискальный признак.
 *
 * Сумма чека — главное число панели, и набрана она тем же начертанием,
 * что итог чека на экране продажи: кассир сверяет её с бумагой в руке.
 */
@Composable
internal fun RefundSummary(basis: FiscalDocumentResponse, journal: ReturnJournalTexts, onBack: () -> Unit) {
    val texts = LocalStrings.current
    // Жест «назад» на Android ведёт туда же, куда стрелка, — к списку чеков.
    SystemBack(enabled = true, onBack = onBack)
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Назад к списку: на узком окне панель стоит вместо списка чеков,
        // и выбрать другое основание иначе нечем.
        IconButton(onClick = onBack) { Icon(AppIcons.back, contentDescription = journal.backToList) }
        // Номер — тот, что стоит на бумажном чеке покупателя: его касса
        // присваивает сама. Здесь стоял номер от ОФД, то есть фискальный
        // признак, и заголовок расходился со списком рядом и с бумагой.
        Text(
            text = "${journal.basis}: ${texts.returns.receiptNo} ${basis.number ?: Glyphs.DASH}",
            style = MaterialTheme.typography.titleMedium
        )
    }
    Text(
        text = journal.receiptTotal,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    // Одной строкой и целиком: от миллиарда сумма переносилась посреди числа.
    MoneyText(Money.formatTiyn(basis.totalAmount), Modifier.fillMaxWidth(), MoneyStyle.hero)
    Text(
        text = "${journal.fiscalSign}: ${basis.fiscalSign ?: basis.autonomousSign ?: Glyphs.DASH}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/**
 * Сумма возврата: моноширинно и вправо, как и всякая сумма в кассе.
 *
 * Поле тянется на остаток ряда, а «Весь чек» уходит под него, когда
 * кассе тесно, — не сжимается до обрывка.
 */
@Composable
internal fun RefundAmountRow(journal: ReturnJournalTexts, draft: RefundDraft, actions: RefundActions) {
    WrapRow(modifier = Modifier.fillMaxWidth(), spacing = Spacing.snug) {
        MoneyField(
            value = draft.entered,
            label = journal.amount,
            modifier = Modifier.weight(1f).widthIn(min = Sizes.fieldAmount),
            isError = draft.checked is RefundAmount.Rejected,
            onValueChange = actions::enter
        )
        FieldButton(journal.wholeReceipt, FieldButtonKind.Text, onClick = actions::wholeReceipt)
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
