package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.payment.PaymentLines
import kz.mybrain.superkassa.desktop.ui.payment.unsupportedPayments
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

/**
 * Оплата чека.
 *
 * Не самостоятельная карточка, а верхняя часть денежного блока: «чем
 * платят» и «сколько» — один вопрос, и разделять их рамкой значит занять
 * высоту, которой не хватает вводу позиции.
 *
 * Вид оплаты выбирается выпадающим списком: высота кассовой колонки
 * нужнее вводу товара, чем шести всегда развёрнутым плашкам. Оплат может
 * быть несколько — их разбиение по видам живёт в [PaymentLines].
 *
 * Принятые деньги вводятся в денежном блоке, рядом с итогом и сдачей:
 * кассир набирает их, глядя на сумму к оплате.
 */
@Composable
fun PaymentPanel(session: Session, form: SaleForm, total: BigDecimal) {
    val extra = LocalSaleTexts.current
    val short = form.split.cashSum(total).let { cash ->
        cash.signum() > 0 && amount(form.taken).value?.let { it < cash } == true
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        PaymentLines(
            session = session,
            split = form.split,
            total = total,
            unsupportedNote = extra.paymentUnsupported
        )
        Hint(
            problem = when {
                form.split.types.any { it in unsupportedPayments(session) } -> extra.blockPaymentUnsupported
                short -> extra.blockTakenTooSmall
                else -> null
            },
            hint = if (form.split.hasCash) null else extra.takenOnlyCash
        )
    }
}
