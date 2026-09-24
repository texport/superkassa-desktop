package kz.mybrain.superkassa.presentation.analytics.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.domain.analytics.model.SalesOverview
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.common.text.MoneyText
import kz.mybrain.superkassa.presentation.strings.analytics.AnalyticsSalesTexts
import kz.mybrain.superkassa.presentation.theme.StatusColors
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kotlin.math.abs

/**
 * Итоги срока для руководства.
 *
 * Первое, что видит человек, открывший раздел. Первым рядом идёт то,
 * что спрашивают об этой сети первым: выручка, НДС с неё и доля расчётов,
 * прошедших без наличных; вторым — чеки и средний чек.
 * Под числом — изменение к прошлому сроку такой же длины: само число
 * без сравнения не говорит, идёт дело в рост или под уклон.
 *
 * Своих рамок у чисел здесь нет: они стоят на карточке раздела, и рамка
 * вокруг каждого добавляла бы к ней вторую границу. Плитки в рамках
 * остались ниже, у чисел второго ряда.
 */
@Composable
fun SalesOverviewTiles(overview: SalesOverview, texts: AnalyticsSalesTexts, modifier: Modifier = Modifier) {
    OverviewTiles(overview, texts, ChangeLine(texts), modifier)
}

@Composable
private fun OverviewTiles(overview: SalesOverview, texts: AnalyticsSalesTexts, line: ChangeLine, modifier: Modifier) {
    OverviewRow(modifier) {
        OverviewTile(texts.revenue, line.of(overview.revenueChange), Modifier.weight(1f)) {
            HeroNumber(Money.formatTiyn(overview.revenue))
        }
        OverviewTile(vatLabel(overview, texts), line.of(overview.taxChange), Modifier.weight(1f)) {
            HeroNumber(Money.formatTiyn(overview.tax))
        }
        OverviewTile(texts.cashless, line.of(overview.cashlessChange, points = true), Modifier.weight(1f)) {
            HeroNumber(shareText(overview.cashless))
        }
        OverviewTile(texts.receipts, line.of(overview.receiptsChange), Modifier.weight(1f)) {
            HeroNumber(Money.count(overview.receiptCount))
        }
        OverviewTile(texts.average, line.of(overview.averageChange), Modifier.weight(1f)) {
            HeroNumber(Money.formatTiyn(overview.average))
        }
        // Третье место второго ряда заполняется: два числа на три места разъезжались.
        OverviewTile(texts.net, line.of(overview.netChange), Modifier.weight(1f)) {
            HeroNumber(Money.formatTiyn(overview.net))
        }
    }
}

/**
 * Одно главное число: само число, изменение под ним и подпись.
 *
 * Строка изменения стоит, даже когда сравнивать не с чем: без неё
 * плитки в ряду вышли бы разной высоты, и ряд читался бы сломанным.
 */
@Composable
private fun OverviewTile(label: String, change: Change, modifier: Modifier = Modifier, value: @Composable () -> Unit) {
    Column(
        modifier = modifier.widthIn(min = Sizes.salesTile),
        verticalArrangement = Arrangement.spacedBy(Spacing.inline)
    ) {
        value()
        change.text?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = changeTone(change.value), maxLines = 1)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Ряд итогов: по три числа, переносом, а не сжатием. */
@Composable
private fun OverviewRow(modifier: Modifier, content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
        maxItemsInEachRow = OVERVIEW_IN_ROW,
        content = content
    )
}

/** Подпись налога: нуль его назван словами, а не оставлен голым «0,00 ₸». */
private fun vatLabel(overview: SalesOverview, texts: AnalyticsSalesTexts): String =
    if (overview.taxCharged) texts.vat else texts.vatNone

/** Изменение к прошлому сроку: число для цвета и строка под главным числом. */
private class Change(val value: Int?, val text: String?)

/**
 * Как читается изменение под числом.
 *
 * Сравнивать не с чем — кабинет о прошлом сроке не ответил — прочерк:
 * строка стоит, и ряд не ломается.
 *
 * `points` — изменение меряется процентными пунктами, а не процентами:
 * так считается изменение доли — процент от процента был бы другой
 * величиной и обманывал бы читающего.
 */
private class ChangeLine(private val texts: AnalyticsSalesTexts) {
    fun of(change: Int?, points: Boolean = false): Change = Change(change, text(change, points))

    private fun text(change: Int?, points: Boolean): String? {
        if (change == null) return "${Glyphs.DASH}${Glyphs.SEPARATOR}${texts.versusPrevious}"
        val arrow = when {
            change > 0 -> "${Glyphs.RISE}${Glyphs.NBSP}"
            change < 0 -> "${Glyphs.FALL}${Glyphs.NBSP}"
            else -> ""
        }
        val unit = if (points) texts.percentPoints else "%"
        return "$arrow${abs(change)}${Glyphs.NBSP}$unit${Glyphs.SEPARATOR}${texts.versusPrevious}"
    }
}

/**
 * Главное число итогов: целиком и одной строкой.
 *
 * Сумма в миллиарды тенге обрывалась многоточием — «98 797 03…»; теперь
 * она встаёт целиком, а не поместившись — ступенью мельче.
 */
@Composable
private fun HeroNumber(text: String) {
    MoneyText(text, style = MaterialTheme.typography.headlineMedium)
}

/** Цвет изменения: рост и падение различают по цвету, а не только по стрелке. */
@Composable
private fun changeTone(change: Int?): Color = when {
    change == null || change == 0 -> MaterialTheme.colorScheme.onSurfaceVariant
    change > 0 -> StatusColors.delivered
    else -> MaterialTheme.colorScheme.error
}

/** Доля в процентах; расчётов за срок не было — прочерк, а не ноль. */
private fun shareText(share: Int?): String =
    share?.let { "$it${Glyphs.NBSP}%" } ?: Glyphs.DASH

/**
 * Сколько главных чисел встаёт в ряд.
 *
 * Три, как и у плиток ниже: сумме в сотни миллионов тенге при крупной
 * шкале нужна треть ширины раздела, а пятое и четвёртое число становятся
 * вторым рядом.
 */
private const val OVERVIEW_IN_ROW = 3
