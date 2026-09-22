package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.Collapsible
import kz.mybrain.superkassa.desktop.ui.components.HeroSumLine
import kz.mybrain.superkassa.desktop.ui.components.MinorSumLine
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.components.SectionHeader
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

/**
 * Итоги чека и сдача.
 *
 * Приподнятая карточка внизу кассовой колонки: слагаемые набраны мелко
 * и приглушённо, итог — самым крупным начертанием денег на экране. Сдача
 * показана так же крупно и вторичной ролью схемы: кассир считает её в уме
 * под взглядом очереди, и ошибка здесь стоит живых денег.
 *
 * Пока оплата не наличными, строки «принято» и «сдача» не показываются
 * вовсе — к безналичному расчёту они отношения не имеют.
 *
 * Блок сворачивается стрелкой, и итог остаётся виден в любом состоянии:
 * свёрнутая панель отдаёт высоту вводу товара, но сумму к оплате кассир
 * должен видеть всегда.
 */
@Composable
fun ReceiptTotals(
    session: Session,
    basket: Basket,
    form: SaleForm,
    total: BigDecimal,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val texts = LocalStrings.current
    val extra = LocalSaleTexts.current
    val discount = amount(form.discount).value
    val markup = amount(form.markup).value
    val taken = amount(form.taken).value
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
        ) {
            SectionHeader(extra.paymentAndTotal, expanded, onToggle)
            // Скидка и наценка на чек показываются только когда заданы:
            // без них сумма позиций — то же число, что итог, а разделитель
            // над пустотой давал на экране две черты подряд.
            val changed = (discount ?: BigDecimal.ZERO) > BigDecimal.ZERO ||
                (markup ?: BigDecimal.ZERO) > BigDecimal.ZERO
            Collapsible(expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.hairline)) {
                    PaymentPanel(session, form, total)
                    if (changed) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.tight))
                        MinorSumLine(extra.itemsSum, Money.format(basket.total))
                        if (discount != null && discount > BigDecimal.ZERO) {
                            MinorSumLine(texts.sale.receiptDiscount, Money.format(discount.negate()))
                        }
                        if (markup != null && markup > BigDecimal.ZERO) {
                            MinorSumLine(texts.sale.receiptMarkup, Money.format(markup))
                        }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.tight))
            HeroSumLine(texts.sale.total, Money.format(total), MaterialTheme.colorScheme.onSurface)
            // Принятые деньги и сдача — часть денежного итога, а не оплаты:
            // кассир вводит их, глядя на сумму к оплате, и обе цифры должны
            // стоять рядом.
            Collapsible(expanded && form.split.hasCash) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.hairline)) {
                    val cash = form.split.cashSum(total)
                    TakenField(form, short = taken != null && taken < cash)
                    if (taken != null) ChangeLine(taken, cash)
                }
            }
        }
    }
}

/** Сколько денег дал покупатель: от этого считается сдача. */
@Composable
private fun TakenField(form: SaleForm, short: Boolean) {
    val texts = LocalStrings.current
    val taken = amount(form.taken)
    OutlinedTextField(
        value = form.taken,
        onValueChange = { form.taken = it },
        label = { Text(texts.sale.taken) },
        singleLine = true,
        textStyle = MoneyStyle.row,
        isError = form.taken.isNotBlank() && (taken.value == null || short),
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Сдача.
 *
 * «Сдачи нет» написано словами, а не нулём: ноль в этой строке кассир
 * прочитал бы как «ещё не посчитано».
 */
@Composable
private fun ChangeLine(taken: BigDecimal, cashSum: BigDecimal) {
    val extra = LocalSaleTexts.current
    val change = changeOf(taken, cashSum) ?: return
    if (change.signum() == 0) {
        Text(
            text = extra.changeNone,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        HeroSumLine(extra.change, Money.format(change), MaterialTheme.colorScheme.primary)
    }
}
