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
import kz.mybrain.superkassa.desktop.ui.cabinet.cabinetSum
import kz.mybrain.superkassa.desktop.ui.components.Chip
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
        OverviewTile(cabinetSum(overview.revenue), texts.revenue, overview.revenueChange, texts, Modifier.weight(1f))
        OverviewTile(cabinetSum(overview.tax), texts.vat, overview.taxChange, texts, Modifier.weight(1f))
        OverviewTile(
            value = shareText(overview.cashless),
            label = texts.cashless,
            change = overview.cashlessChange,
            texts = texts,
            modifier = Modifier.weight(1f),
            points = true
        )
        OverviewTile(
            value = Money.count(overview.receiptCount),
            label = texts.receipts,
            change = overview.receiptsChange,
            texts = texts,
            modifier = Modifier.weight(1f)
        )
        OverviewTile(cabinetSum(overview.average), texts.average, overview.averageChange, texts, Modifier.weight(1f))
        // Третье место второго ряда заполняется, а не остаётся пустым:
        // два числа в ряду на три места разъезжались по левому краю,
        // и ровная сетка итогов ломалась ровно посередине карточки.
        OverviewTile(cabinetSum(overview.net), texts.net, overview.netChange, texts, Modifier.weight(1f))
    }
}

/**
 * Состояние сети касс одной строкой плашек.
 *
 * Отвечает на вопрос проверяющего: все ли кассы на связи и не работает
 * ли часть сети мимо БФД. «На связи» считается по чекам срока — своей
 * ручки о связи у кабинета нет, — а «молчат» берётся от числа касс
 * компании, чтобы касса, не приславшая ни одного документа, не исчезала
 * из счёта вместе со своей строкой.
 *
 * Цветом красится только то, что требует работы: ноль молчащих касс
 * красным читался бы как беда, которой нет. Плашки о заблокированных
 * кассах здесь нет вовсе — сводка кабинета о блокировках не отвечает,
 * и выдумывать это число нельзя.
 *
 * @param register касса, которой ограничен отбор; `null` — вся сеть.
 *   В окне одной кассы плашек о числе касс нет: на вопрос о сети внутри
 *   окна про одну машину не отвечают.
 */
@Composable
fun SalesNetworkPlates(
    view: SalesView,
    texts: AnalyticsSalesTexts,
    modifier: Modifier = Modifier,
    register: String? = null
) {
    val silent = silentRegisters(view.summary, view.registers)
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
    ) {
        if (register == null) {
            val selling = sellingRegisters(view.registers)
            Plate(selling, texts.online, good(selling))
            Plate(silent, texts.silent, attention(silent))
        }
        Plate(view.summary.openShiftCount, texts.openShifts, MaterialTheme.colorScheme.onSurface)
        Plate(view.summary.offlineCount, texts.offline, attention(view.summary.offlineCount))
        Plate(view.summary.queuedCount, texts.queuedCount, waiting(view.summary.queuedCount))
        Plate(view.summary.unknownCount, texts.unknownCount, attention(view.summary.unknownCount))
    }
}

/** Одна плашка сети: число и то, чего оно касается. */
@Composable
private fun Plate(count: Int, label: String, tone: Color) {
    Chip(text = "${Money.count(count)}${Glyphs.SEPARATOR}$label", color = tone)
}

/** Цвет доброй вести; ноль касс на связи вестью не является. */
@Composable
private fun good(count: Int): Color =
    if (count > 0) StatusColors.delivered else MaterialTheme.colorScheme.onSurfaceVariant

/** Цвет числа, которое требует работы; ноль такого не требует. */
@Composable
private fun attention(count: Int): Color =
    if (count > 0) StatusColors.refused else MaterialTheme.colorScheme.onSurfaceVariant

/** То же для того, что доедет само: очередь — это ожидание, а не отказ. */
@Composable
private fun waiting(count: Int): Color =
    if (count > 0) StatusColors.pending else MaterialTheme.colorScheme.onSurfaceVariant

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
    value: String,
    label: String,
    change: Int?,
    texts: AnalyticsSalesTexts,
    modifier: Modifier = Modifier,
    points: Boolean = false
) {
    Column(
        modifier = modifier.widthIn(min = Sizes.salesTile),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
