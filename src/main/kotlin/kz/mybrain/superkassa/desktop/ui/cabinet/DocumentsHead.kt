package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.server.cabinet.DocumentsOverview
import kz.mybrain.superkassa.desktop.ui.components.CounterTile
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Сколько чего у кассы накопилось по данным БФД.
 *
 * Ряд крупных чисел без карточки вокруг: числа и так стоят под шапкой
 * экрана, и рамка вокруг четырёх из них добавляла бы линий, а не смысла.
 *
 * Над числами сказано, за какой они срок. Счёт кабинет отдаёт за всё
 * время, а список под ним отобран выбранным сроком — и без этой строки
 * экран противоречил сам себе: «100 · Чеки» вверху и «Документов нет»
 * ниже, на той же картинке. Слово берётся у ряда сроков под числами:
 * там оно уже написано, и двух разных «за всё время» на одном экране
 * быть не должно.
 */
@Composable
internal fun DocumentCounters(overview: DocumentsOverview?, texts: CabinetTexts, allTime: String) {
    val counts = overview ?: return
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
    ) {
        Text(
            text = allTime,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.roomy),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
            CounterTile(counts.receiptsCount.toString(), texts.receipts)
            CounterTile(counts.shiftsCount.toString(), texts.shifts)
            CounterTile(counts.reportsCount.toString(), texts.reports)
            CounterTile(counts.cashMovementsCount.toString(), texts.cashMovements)
        }
    }
}
