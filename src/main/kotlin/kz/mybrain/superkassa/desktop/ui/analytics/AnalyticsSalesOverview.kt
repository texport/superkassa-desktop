package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.ui.adaptive.MoneyText
import kz.mybrain.superkassa.desktop.ui.cabinet.cabinetSum
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsSalesTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors
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
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug),
        maxItemsInEachRow = OVERVIEW_IN_ROW
    ) {
        OverviewTile(texts.revenue, overview.revenueChange, texts, Modifier.weight(1f)) {
            HeroNumber(cabinetSum(overview.revenue))
        }
        OverviewTile(texts.vat, overview.taxChange, texts, Modifier.weight(1f)) { HeroNumber(cabinetSum(overview.tax)) }
        OverviewTile(texts.cashless, overview.cashlessChange, texts, Modifier.weight(1f), points = true) {
            HeroNumber(shareText(overview.cashless))
        }
        OverviewTile(texts.receipts, overview.receiptsChange, texts, Modifier.weight(1f)) {
            HeroNumber(Money.count(overview.receiptCount))
        }
        OverviewTile(texts.average, overview.averageChange, texts, Modifier.weight(1f)) {
            HeroNumber(cabinetSum(overview.average))
        }
        // Третье место второго ряда заполняется, а не остаётся пустым:
        // два числа в ряду на три места разъезжались по левому краю,
        // и ровная сетка итогов ломалась ровно посередине карточки.
        OverviewTile(texts.net, overview.netChange, texts, Modifier.weight(1f)) { HeroNumber(cabinetSum(overview.net)) }
    }
}

/**
 * Одно главное число: само число, изменение под ним и подпись.
 *
 * Строка изменения стоит всегда, даже когда сравнивать не с чем: без неё
 * плитки в ряду вышли бы разной высоты, и ряд читался бы сломанным.
 *
 * @param points изменение меряется процентными пунктами, а не процентами:
 *   так считается изменение доли — процент от процента был бы другой
 *   величиной и обманывал бы читающего.
 */
@Composable
private fun OverviewTile(
    label: String,
    change: Int?,
    texts: AnalyticsSalesTexts,
    modifier: Modifier = Modifier,
    points: Boolean = false,
    value: @Composable () -> Unit
) {
    Column(
        modifier = modifier.widthIn(min = Sizes.salesTile),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
    ) {
        value()
        Text(
            text = changeText(change, texts, points),
            style = MaterialTheme.typography.labelMedium,
            color = changeTone(change),
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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

/** Изменение словами: стрелка, число и то, чем оно меряется. */
private fun changeText(change: Int?, texts: AnalyticsSalesTexts, points: Boolean): String {
    if (change == null) return "${Glyphs.DASH}${Glyphs.SEPARATOR}${texts.versusPrevious}"
    val arrow = when {
        change > 0 -> "${Glyphs.RISE}${Glyphs.NBSP}"
        change < 0 -> "${Glyphs.FALL}${Glyphs.NBSP}"
        else -> ""
    }
    val unit = if (points) texts.percentPoints else "%"
    return "$arrow${abs(change)}${Glyphs.NBSP}$unit${Glyphs.SEPARATOR}${texts.versusPrevious}"
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
