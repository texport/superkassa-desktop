package kz.mybrain.superkassa.presentation.kassa.sale.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.designsystem.section.CollapsibleSection
import kz.mybrain.superkassa.designsystem.section.SectionTitle
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.text.MoneyText
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.theme.type.MoneyStyle
import kz.mybrain.superkassa.domain.kassa.model.entry.amount
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleForm
import kz.mybrain.superkassa.domain.kassa.model.sale.changeOf
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.kassa.field.MoneyField
import kz.mybrain.superkassa.presentation.kassa.payment.PaymentActions
import kz.mybrain.superkassa.presentation.kassa.sale.LocalSaleTexts
import kz.mybrain.superkassa.presentation.kassa.sale.SaleUiState
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Итог чека — «К оплате» самым крупным начертанием денег на экране.
 *
 * Закреплён внизу кассы вместе с «Пробить чек» ([CheckoutPanel]): сумму
 * к оплате кассир видит всегда, сколько бы ни было позиций и оплат.
 * Сами оплаты — в прокручиваемой части кассы, сразу за вводом позиции
 * ([PaymentCard]): прежде они прокручивались внутри закреплённого блока,
 * и на планшете лёжа поле «Принято» уходило под кнопку, а вводу позиции
 * оставалась полоска со штрихкодом.
 */
@Composable
internal fun ReceiptTotal(state: SaleUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.inline)
    ) {
        SectionTitle(textsOf(LocalLanguage.current).kassa.checkout.toPay)
        val color = MaterialTheme.colorScheme.onSurface
        MoneyText(Money.formatTiyn(state.total), Modifier.fillMaxWidth(), MoneyStyle.hero, color)
    }
}

/**
 * Оплата чека — виды оплаты, принятые деньги и сдача одним блоком.
 *
 * «Чем платят», «сколько дали» и «сколько сдачи» — один расчёт, и кассир
 * ведёт его в одном месте. Сдача показана так же
 * крупно и вторичной ролью схемы: кассир считает её в уме под взглядом
 * очереди, и ошибка здесь стоит живых денег. Пока оплата не наличными,
 * строк «принято» и «сдача» нет вовсе. Блок сворачивается стрелкой
 * в заголовке, как остальные разделы кассы.
 */
@Composable
internal fun PaymentCard(
    state: SaleUiState,
    payments: PaymentActions,
    expanded: Boolean,
    onToggle: () -> Unit,
    onTaken: (String) -> Unit
) {
    TillCard {
        CollapsibleSection(title = LocalSaleTexts.current.payment, expanded = expanded, onToggle = onToggle) {
            PaymentPanel(state, payments)
            CashTaken(state.form, state.total, onTaken)
        }
    }
}

/**
 * Принятые наличные и сдача — рядом с суммой к оплате: кассир вводит их,
 * глядя на неё. Без наличных в оплате их нет.
 */
@Composable
private fun CashTaken(form: SaleForm, total: Long, onTaken: (String) -> Unit) {
    if (!form.split.hasCash) return
    val taken = amount(form.taken).tiyn
    val cash = form.split.cashSum(total)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.inline)) {
        TakenField(form.taken, short = taken != null && taken < cash, onTaken)
        if (taken != null) ChangeLine(taken, cash)
    }
}

/** Подпись и крупная сумма под ней, прижатая вправо. */
@Composable
private fun SumLine(title: String, sum: Long, color: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    MoneyText(Money.formatTiyn(sum), Modifier.fillMaxWidth(), MoneyStyle.hero, color)
}

/** Сколько денег дал покупатель: от этого считается сдача. */
@Composable
private fun TakenField(taken: String, short: Boolean, onTaken: (String) -> Unit) {
    val texts = LocalStrings.current
    MoneyField(
        value = taken,
        label = texts.receipt.taken,
        modifier = Modifier.fillMaxWidth(),
        isError = taken.isNotBlank() && (amount(taken).tiyn == null || short),
        onValueChange = onTaken
    )
}

/**
 * Сдача.
 *
 * «Сдачи нет» написано словами, а не нулём: ноль в этой строке кассир
 * прочитал бы как «ещё не посчитано».
 */
@Composable
private fun ChangeLine(taken: Long, cashSum: Long) {
    val extra = LocalSaleTexts.current
    val change = changeOf(taken, cashSum) ?: return
    if (change == 0L) {
        Text(
            text = extra.changeNone,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        SumLine(extra.change, change, MaterialTheme.colorScheme.primary)
    }
}
