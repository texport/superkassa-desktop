package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.payment.PaymentLines
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
 *
 * Помехи здесь не называются: причина, по которой чек пробить нельзя,
 * стоит под кнопкой и всегда одна. Прежде та же красная строка стояла
 * ещё и здесь, и «Принято меньше итога» кассир читал на одном экране
 * дважды — а при непринимаемом виде оплаты трижды, считая серое
 * пояснение о погасших видах.
 */
@Composable
fun PaymentPanel(session: Session, form: SaleForm, total: BigDecimal) {
    val extra = LocalSaleTexts.current
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
        // Остаётся только правило: принятые деньги и сдачу спрашивают
        // при наличных, и об этом сказано до того, как кассир их искал.
        Hint(problem = null, hint = if (form.split.hasCash) null else extra.takenOnlyCash)
    }
}
