package kz.mybrain.superkassa.presentation.kassa.refund.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import kz.mybrain.superkassa.designsystem.button.FieldButton
import kz.mybrain.superkassa.designsystem.button.FieldButtonKind
import kz.mybrain.superkassa.designsystem.keyboard.SystemBack
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.text.MoneyText
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.theme.type.MoneyStyle
import kz.mybrain.superkassa.domain.document.model.number
import kz.mybrain.superkassa.domain.kassa.model.payment.SplitIssue
import kz.mybrain.superkassa.domain.kassa.model.refund.RefundAmount
import kz.mybrain.superkassa.domain.kassa.model.refund.RefundDraft
import kz.mybrain.superkassa.domain.kassa.model.refund.RefundProblem
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.kassa.field.MoneyField
import kz.mybrain.superkassa.presentation.kassa.refund.RefundActions
import kz.mybrain.superkassa.strings.api.journal.ReturnJournalTexts
import kz.mybrain.superkassa.strings.api.kassa.PaymentTexts
import kz.mybrain.superkassa.strings.api.textsOf

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
 * Поле тянется на остаток ряда панели, «Весь чек» стоит рядом: края поля
 * совпадают с краями полей оплаты под ним.
 */
@Composable
internal fun RefundAmountRow(journal: ReturnJournalTexts, draft: RefundDraft, actions: RefundActions) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap)) {
        // Под полем — то, что о сумме нужно знать сейчас: почему она не годится,
        // а когда годится и меньше чека — что вернётся только часть. При
        // возврате всего чека пояснять нечего, и строка не отнимает высоту
        // у панели в малом окне.
        val checked = draft.checked
        val rejected = checked as? RefundAmount.Rejected
        val partial = (checked as? RefundAmount.Ready)?.let { it.tiyn < draft.total } == true
        MoneyField(
            value = draft.entered,
            label = journal.amount,
            modifier = Modifier.weight(1f),
            isError = rejected != null,
            supportingText = rejected?.let { problemText(it.reason, journal) }
                ?: textsOf(LocalLanguage.current).kassa.checkout.refundPart.takeIf { partial },
            onValueChange = actions::enter
        )
        FieldButton(journal.wholeReceipt, FieldButtonKind.Text, onClick = actions::wholeReceipt)
    }
}

/**
 * Чем не годится разбиение оплат возврата, если не годится.
 *
 * О сумме — частичный возврат и отказ по ней — сказано под самим полем
 * суммы: прежде пояснение стояло под «Добавить оплату» и читалось
 * как правило оплат.
 */
@Composable
internal fun RefundHints(split: SplitIssue?, payment: PaymentTexts) {
    val problem = when {
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
