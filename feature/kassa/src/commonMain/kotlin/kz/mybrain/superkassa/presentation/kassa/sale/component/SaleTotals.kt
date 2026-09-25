package kz.mybrain.superkassa.presentation.kassa.sale.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
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
 * Оплата чека: чем платят и сколько каждым видом.
 *
 * Стоит в прокручиваемой части кассы, а не у кнопки: строк оплаты бывает
 * пять, и прибитые к низу, они отнимали у ввода позиции всю высоту.
 * Сворачивается вместе с принятыми деньгами — это один раздел кассы.
 */
@Composable
internal fun PaymentCard(state: SaleUiState, actions: PaymentActions, expanded: Boolean, onToggle: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.inline)
        ) {
            SectionHeader(LocalSaleTexts.current.payment, expanded, onToggle)
            Collapsible(expanded) { PaymentPanel(state, actions) }
        }
    }
}

/**
 * Итог чека и сдача.
 *
 * Внизу кассы, в блоке оплаты ([CheckoutPanel]): «К оплате» — самым
 * крупным начертанием денег на экране.
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
internal fun ReceiptTotals(form: SaleForm, total: Long, expanded: Boolean, onTaken: (String) -> Unit) {
    val texts = LocalStrings.current
    val taken = amount(form.taken).tiyn
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.inline)
    ) {
        SumLine(textsOf(LocalLanguage.current).kassa.checkout.toPay, total, MaterialTheme.colorScheme.onSurface)
        // Принятые деньги и сдача — часть денежного итога, а не оплаты:
        // кассир вводит их, глядя на сумму к оплате, и обе цифры должны
        // стоять рядом.
        Collapsible(expanded && form.split.hasCash) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.inline)) {
                val cash = form.split.cashSum(total)
                TakenField(form.taken, short = taken != null && taken < cash, onTaken)
                if (taken != null) ChangeLine(taken, cash)
            }
        }
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
        label = texts.sale.taken,
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
