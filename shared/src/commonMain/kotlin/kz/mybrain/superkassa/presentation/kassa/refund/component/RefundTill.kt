package kz.mybrain.superkassa.presentation.kassa.refund.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.domain.kassa.model.payment.paymentEntries
import kz.mybrain.superkassa.domain.kassa.model.refund.RefundDraft
import kz.mybrain.superkassa.domain.kassa.model.refund.ReturnKind
import kz.mybrain.superkassa.domain.kassa.model.refund.drawerShortage
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.common.format.fill
import kz.mybrain.superkassa.presentation.kassa.payment.PaymentActions
import kz.mybrain.superkassa.presentation.kassa.payment.PaymentLines
import kz.mybrain.superkassa.presentation.kassa.refund.ReturnsUiState
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.journal.ReturnJournalTexts
import kz.mybrain.superkassa.presentation.strings.kassa.action
import kz.mybrain.superkassa.presentation.strings.kassa.paymentTexts
import kz.mybrain.superkassa.presentation.strings.kassa.saleTexts
import kz.mybrain.superkassa.presentation.theme.size.KassaLayout
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Касса возврата: сумма и виды оплаты.
 *
 * Во всю ширину панели, как любая форма: колонка шириной чтения
 * оставляла на широком окне пустую треть панели.
 */
@Composable
internal fun RefundTill(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
        content = content
    )
}

/**
 * Главное действие возврата: той же высоты, что и главные кнопки других
 * экранов, и той же ширины, что касса над ней.
 */
@Composable
internal fun RefundButton(kind: ReturnKind, enabled: Boolean, onClick: () -> Unit) {
    Button(
        enabled = enabled,
        onClick = onClick,
        // Цель нажатия кассы — выше обычной кнопки, как «Пробить чек».
        modifier = Modifier.fillMaxWidth().heightIn(min = KassaLayout.mainAction)
    ) {
        Text(kind.action(LocalStrings.current.returns))
    }
}

/**
 * Чем отдают деньги и хватает ли их в ящике.
 *
 * Возврат отдают тем же набором, каким платили: часть на карту, часть
 * из ящика. Сумма разбивается от суммы возврата, а не от итога
 * чека-основания. Почему вид оплаты в списке погас — теми же словами,
 * что и на продаже: погасшая строка без объяснения читается как поломка.
 * О нехватке денег в ящике говорится под видами оплаты и до выдачи,
 * а не отказом узла после.
 */
@Composable
internal fun RefundMoney(
    state: ReturnsUiState,
    draft: RefundDraft,
    payments: PaymentActions,
    journal: ReturnJournalTexts
) {
    val language = LocalLanguage.current
    PaymentLines(
        split = draft.split,
        entries = paymentEntries(state.paymentTypes),
        total = draft.readyTiyn,
        actions = payments,
        unsupportedNote = saleTexts(language).paymentUnsupported
    )
    RefundNote(
        drawerShortage(state.kind, state.cashInDrawer, draft.split.cashSum(draft.readyTiyn))
            ?.let { journal.drawerShort.fill(Money.formatTiyn(it)) }
    )
    RefundHints(draft.split.issue(draft.readyTiyn), paymentTexts(language))
}
