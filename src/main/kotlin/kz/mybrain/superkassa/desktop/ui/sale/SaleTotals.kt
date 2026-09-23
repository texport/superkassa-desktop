package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.adaptive.MoneyText
import kz.mybrain.superkassa.desktop.ui.components.Collapsible
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.components.MoneyField
import kz.mybrain.superkassa.desktop.ui.components.SectionHeader
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

/**
 * Оплата чека: чем платят и сколько каждым видом.
 *
 * Стоит в прокручиваемой части кассы, а не у кнопки: строк оплаты бывает
 * пять, и прибитые к низу, они отнимали у ввода позиции всю высоту.
 * Сворачивается вместе с принятыми деньгами — это один раздел кассы.
 */
@Composable
fun PaymentCard(
    session: Session,
    form: SaleForm,
    total: BigDecimal,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
        ) {
            SectionHeader(LocalSaleTexts.current.payment, expanded, onToggle)
            Collapsible(expanded) { PaymentPanel(session, form, total) }
        }
    }
}

/**
 * Итог чека и сдача.
 *
 * Внизу кассы, без рамки: итог — самым крупным начертанием денег на экране.
 * Сдача показана так же крупно и вторичной ролью схемы: кассир считает её
 * в уме под взглядом очереди, и ошибка здесь стоит живых денег. Суммы
 * набраны целиком одной строкой: от миллиарда они переносились посреди
 * числа. Карточка вокруг итога была третьим видом карточки в одной
 * колонке и обрезала сумму, не поместившуюся даже малой ступенью, —
 * без рамки такая сумма видна целиком.
 *
 * Пока оплата не наличными, строки «принято» и «сдача» не показываются
 * вовсе — к безналичному расчёту они отношения не имеют. Свёрнутая оплата
 * прячет их вместе с собой, а итог остаётся: сумму к оплате кассир
 * должен видеть всегда.
 */
@Composable
fun ReceiptTotals(form: SaleForm, total: BigDecimal, expanded: Boolean) {
    val texts = LocalStrings.current
    val taken = amount(form.taken).value
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SumLine(texts.sale.total, total, MaterialTheme.colorScheme.onSurface)
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

/** Подпись и крупная сумма под ней, прижатая вправо. */
@Composable
private fun SumLine(title: String, sum: BigDecimal, color: Color) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    MoneyText(Money.format(sum), Modifier.fillMaxWidth(), MoneyStyle.hero, color)
}

/** Сколько денег дал покупатель: от этого считается сдача. */
@Composable
private fun TakenField(form: SaleForm, short: Boolean) {
    val texts = LocalStrings.current
    val taken = amount(form.taken)
    MoneyField(
        value = form.taken,
        label = texts.sale.taken,
        modifier = Modifier.fillMaxWidth(),
        isError = form.taken.isNotBlank() && (taken.value == null || short),
        onValueChange = { form.taken = it }
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
        SumLine(extra.change, change, MaterialTheme.colorScheme.primary)
    }
}
