package kz.mybrain.superkassa.presentation.kassa.sale.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.kassa.model.payment.paymentEntries
import kz.mybrain.superkassa.presentation.kassa.payment.PaymentActions
import kz.mybrain.superkassa.presentation.kassa.payment.PaymentLines
import kz.mybrain.superkassa.presentation.kassa.sale.LocalSaleTexts
import kz.mybrain.superkassa.presentation.kassa.sale.SaleUiState

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
internal fun PaymentPanel(state: SaleUiState, actions: PaymentActions) {
    val extra = LocalSaleTexts.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
    ) {
        PaymentLines(
            split = state.form.split,
            entries = paymentEntries(state.paymentTypes),
            total = state.total,
            actions = actions,
            unsupportedNote = extra.paymentUnsupported
        )
        // Остаётся только правило: принятые деньги и сдачу спрашивают
        // при наличных, и об этом сказано до того, как кассир их искал.
        // С наличными строки нет вовсе: пустая, она держала место между
        // «Добавить оплату» и «Принято», и отступ там был втрое шире прочих.
        if (!state.form.split.hasCash) Hint(problem = null, hint = extra.takenOnlyCash)
    }
}
