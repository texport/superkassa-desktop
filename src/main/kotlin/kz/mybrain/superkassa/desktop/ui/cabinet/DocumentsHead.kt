package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.server.cabinet.DocumentsOverview
import kz.mybrain.superkassa.desktop.ui.components.CounterTile
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Сколько чего у кассы накопилось по данным ОФД.
 *
 * Ряд крупных чисел без карточки вокруг: числа и так стоят под шапкой
 * экрана, и рамка вокруг четырёх из них добавляла бы линий, а не смысла.
 */
@Composable
internal fun DocumentCounters(overview: DocumentsOverview?, texts: CabinetTexts) {
    val counts = overview ?: return
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
