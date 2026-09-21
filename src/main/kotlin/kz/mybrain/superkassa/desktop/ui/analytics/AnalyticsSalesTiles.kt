package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.server.cabinet.SalesDelivery
import kz.mybrain.superkassa.desktop.server.cabinet.SalesSummary
import kz.mybrain.superkassa.desktop.ui.cabinet.cabinetSum
import kz.mybrain.superkassa.desktop.ui.components.CounterTile
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsSalesTexts
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Главные числа срока плитками.
 *
 * Верхний ряд — то, ради чего владелец открыл раздел: выручка, чеки,
 * средний чек, возвраты и налог. Число набрано крупной шкалой, подпись —
 * служебной: на экране должно быть видно с одного взгляда, что здесь
 * главное, а что его поясняет.
 *
 * Нижний ряд — хозяйство за теми же числами: сколько касс работало,
 * сколько смен открыто сейчас, сколько документов пробито автономно
 * и сколько ещё не доехало. Они набраны шкалой счётчика — той же, что
 * и остальные счётчики приложения, — и с главными числами не спорят.
 *
 * Ряд переносится, а не сжимается. Пятью долями ширины плитка выручки
 * получала меньше, чем нужно её числу, и главное число экрана выходило
 * обрезанным: «128 456 00…» вместо ста двадцати восьми миллионов тенге.
 * Больше трёх главных чисел в ряд не ставится: перенос по одной плитке
 * оставлял последнюю растянутой на всю ширину, и ряд читался кривым.
 */
@Composable
fun SalesTiles(summary: SalesSummary, texts: AnalyticsTexts, modifier: Modifier = Modifier) {
    val sales = texts.sales
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug),
            maxItemsInEachRow = HERO_IN_ROW
        ) {
            HeroTile(cabinetSum(summary.revenue), sales.revenue, Modifier.weight(1f))
            HeroTile(summary.receiptCount.toString(), sales.receipts, Modifier.weight(1f))
            HeroTile(cabinetSum(summary.average), sales.average, Modifier.weight(1f))
            HeroTile(cabinetSum(summary.refunds), sales.refunds, Modifier.weight(1f))
            HeroTile(cabinetSum(summary.tax), sales.tax, Modifier.weight(1f))
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            CounterTile(summary.cashRegisterCount.toString(), texts.kkmCount)
            CounterTile(summary.openShiftCount.toString(), sales.openShifts)
            CounterTile(summary.offlineCount.toString(), sales.offline)
            CounterTile(summary.queuedCount.toString(), sales.queuedCount)
            CounterTile(summary.unknownCount.toString(), sales.unknownCount)
        }
    }
}

/**
 * Покупка у населения: чеки, выплаченное и возвращённое в кассу.
 *
 * Стоит в стороне от главных чисел намеренно. Покупка — не продажа:
 * касса не получает деньги, а выдаёт их, и плитка «Выплачено» в одном
 * ряду с выручкой читалась бы как ещё одна выручка. Названия самой
 * покупки и её возврата взяты из набора кабинета — те же, что стоят
 * в журнале документов.
 *
 * Шкала здесь на ступень мельче, чем у выручки: числа важные, но
 * не главные, и спорить с ними за внимание им не за что.
 */
@Composable
fun SalesPurchaseTiles(
    summary: SalesSummary,
    texts: AnalyticsSalesTexts,
    cabinet: CabinetTexts,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        MinorTile(summary.purchaseCount.toString(), texts.receipts, Modifier.weight(1f))
        MinorTile(cabinetSum(summary.purchases), texts.paidOut, Modifier.weight(1f))
        MinorTile(cabinetSum(summary.purchaseRefunds), cabinet.operationPurchaseReturn, Modifier.weight(1f))
    }
}

/**
 * Состояние доставки документов за срок.
 *
 * Отбракованное покрашено ролью отказа: доставленное и стоящее
 * в очереди доедут сами, а отбракованное — единственное, что требует
 * работы владельца, и найти его глазами он должен сразу.
 */
@Composable
fun SalesDeliveryTiles(delivery: SalesDelivery, texts: AnalyticsSalesTexts, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        StateTile(delivery.delivered, texts.delivered, StatusColors.delivered, Modifier.weight(1f))
        StateTile(delivery.queued, texts.queued, StatusColors.pending, Modifier.weight(1f))
        // Неизвестное стоит рядом с очередью, но цветом их не путаем:
        // об очереди известно, что ответа ждут, а здесь не известно ничего.
        StateTile(delivery.unknown, texts.unknown, MaterialTheme.colorScheme.onSurfaceVariant, Modifier.weight(1f))
        StateTile(delivery.rejected, texts.rejected, StatusColors.refused, Modifier.weight(1f))
        StateTile(delivery.offline, texts.offline, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f))
    }
}

/** Плитка главного числа: крупная шкала и обычный цвет текста. */
@Composable
private fun HeroTile(value: String, label: String, modifier: Modifier = Modifier) {
    Tile(value, label, MaterialTheme.typography.headlineMedium, MaterialTheme.colorScheme.onSurface, modifier)
}

/**
 * Сколько главных чисел встаёт в ряд.
 *
 * Три: сумме в сотни миллионов тенге при крупной шкале нужна треть
 * ширины раздела, а вторая строка с двумя плитками читается как ряд,
 * а не как остаток.
 */
private const val HERO_IN_ROW = 3

/** Плитка числа, которое не главное: шкала на ступень мельче выручки. */
@Composable
private fun MinorTile(value: String, label: String, modifier: Modifier = Modifier) {
    Tile(value, label, MaterialTheme.typography.headlineSmall, MaterialTheme.colorScheme.onSurface, modifier)
}

/** Плитка состояния доставки: шкала помельче, а цвет несёт смысл. */
@Composable
private fun StateTile(value: Int, label: String, tone: Color, modifier: Modifier = Modifier) {
    Tile(value.toString(), label, MaterialTheme.typography.headlineSmall, tone, modifier)
}

/** Одна плитка: число, подпись под ним и рамка вокруг. */
@Composable
private fun Tile(value: String, label: String, style: TextStyle, tone: Color, modifier: Modifier) {
    OutlinedCard(modifier = modifier.widthIn(min = Sizes.salesTile)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.snug),
            verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
        ) {
            Text(text = value, style = style, color = tone, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
