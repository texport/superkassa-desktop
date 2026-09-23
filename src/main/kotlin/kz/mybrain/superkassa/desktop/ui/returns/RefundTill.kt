package kz.mybrain.superkassa.desktop.ui.returns

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.adaptive.ContentKind
import kz.mybrain.superkassa.desktop.ui.adaptive.contentWidth
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.payment.PaymentLines
import kz.mybrain.superkassa.desktop.ui.payment.PaymentSplit
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.ReturnJournalTexts
import kz.mybrain.superkassa.desktop.ui.strings.paymentTexts
import kz.mybrain.superkassa.desktop.ui.strings.saleTexts
import kz.mybrain.superkassa.desktop.ui.theme.KassaLayout
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

/**
 * Касса возврата: сумма и виды оплаты.
 *
 * Не шире читаемого: на широком окне поле суммы и выбор оплаты
 * тянулись через всю панель.
 */
@Composable
internal fun RefundTill(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier.contentWidth(ContentKind.Reading),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug),
        content = content
    )
}

/**
 * Главное действие возврата: той же высоты, что и главные кнопки других
 * экранов, и той же ширины, что касса над ней, — а не через всю панель.
 */
@Composable
internal fun RefundButton(kind: ReturnKind, enabled: Boolean, onClick: () -> Unit) {
    Button(
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.contentWidth(ContentKind.Reading).heightIn(min = KassaLayout.mainAction)
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
    session: Session,
    kind: ReturnKind,
    split: PaymentSplit,
    refundSum: BigDecimal,
    journal: ReturnJournalTexts,
    checked: RefundAmount
) {
    PaymentLines(session, split, refundSum, saleTexts(session.language).paymentUnsupported)
    RefundNote(
        drawerShortage(kind, session.cashInDrawer, split.cashSum(refundSum))
            ?.let { journal.drawerShort.format(Money.formatTiyn(it)) }
    )
    RefundHints(journal, checked, split.issue(refundSum), paymentTexts(session.language))
}
