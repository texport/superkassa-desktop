package kz.mybrain.superkassa.presentation.kassa.sale.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.designsystem.section.Collapsible
import kz.mybrain.superkassa.designsystem.section.SectionHeader
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
 * Блок «К оплате» — итог чека и всё, чем за него платят.
 *
 * Внизу кассы, в блоке оплаты ([CheckoutPanel]): «К оплате» — самым
 * крупным начертанием денег на экране; сумму к оплате кассир видит
 * всегда. Под ней, в одном блоке с ней, — виды оплаты, принятые деньги
 * и сдача: прежде виды оплаты стояли отдельной карточкой выше, и кассир
 * рассчитывался в двух местах колонки. Блок сворачивается вниз стрелкой
 * в своём заголовке, как остальные разделы кассы, — остаются итог
 * и «Пробить чек».
 *
 * Сдача показана так же крупно и вторичной ролью схемы: кассир считает её
 * в уме под взглядом очереди, и ошибка здесь стоит живых денег. Суммы
 * набраны целиком одной строкой: от миллиарда они переносились посреди
 * числа. Пока оплата не наличными, строк «принято» и «сдача» нет вовсе —
 * к безналичному расчёту они отношения не имеют.
 */
@Composable
internal fun ReceiptTotals(
    state: SaleUiState,
    payments: PaymentActions,
    expanded: Boolean,
    onToggle: () -> Unit,
    onTaken: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.inline)
    ) {
        SectionHeader(textsOf(LocalLanguage.current).kassa.checkout.toPay, expanded, onToggle)
        val color = MaterialTheme.colorScheme.onSurface
        MoneyText(Money.formatTiyn(state.total), Modifier.fillMaxWidth(), MoneyStyle.hero, color)
        // Оплата прокручивается внутри блока, если ему не хватает высоты:
        // итог над ней и «Пробить чек» под ней остаются на месте.
        Box(modifier = Modifier.weight(1f, fill = false)) {
            Collapsible(expanded) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
                ) {
                    PaymentPanel(state, payments)
                    CashTaken(state.form, state.total, onTaken)
                }
            }
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
