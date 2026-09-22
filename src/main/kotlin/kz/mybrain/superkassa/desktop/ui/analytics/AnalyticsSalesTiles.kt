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
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsSalesTexts
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Возвраты и чистая выручка плитками.
 *
 * Стоят под итогами для руководства и с ними не спорят: выручка, чеки,
 * средний чек, НДС и доля безналичных названы там один раз, а второй раз
 * те же числа на одном экране заставляют владельца сверять, не разные ли
 * они. Здесь остались числа второго вопроса: сколько вернули покупателям
 * и сколько осталось за вычетом возвратов.
 *
 * Счётчиков сети здесь тоже нет — кассы, смены, автономное и очередь
 * собраны плашками в карточке итогов, рядом с числом молчащих касс:
 * состояние сети читают одной строкой, а не двумя разными местами.
 *
 * Ряд переносится, а не сжимается. Пятью долями ширины плитка выручки
 * получала меньше, чем нужно её числу, и главное число экрана выходило
 * обрезанным: «128 456 00…» вместо ста двадцати восьми миллионов тенге.
 */
@Composable
fun SalesTiles(summary: SalesSummary, texts: AnalyticsTexts, modifier: Modifier = Modifier) {
    val sales = texts.sales
    // Шкала на ступень мельче, чем у итогов над ними: эти два числа
    // важные, но не главные, и крупной шкалой они спорили бы с выручкой
    // за внимание.
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        MinorTile(cabinetSum(summary.refunds), sales.refunds, Modifier.weight(1f))
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
        MinorTile(Money.count(summary.purchaseCount), texts.receipts, Modifier.weight(1f))
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
