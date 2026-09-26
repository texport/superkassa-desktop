package kz.mybrain.superkassa.domain.map.usecase

import kz.mybrain.superkassa.domain.map.model.MapPanel
import kz.mybrain.superkassa.domain.map.port.MapMemory

/** Сворачивает или раскрывает часть карты; выбор помнится рабочим местом. */
class FoldMapPanel(private val memory: MapMemory) {

    operator fun invoke(panel: MapPanel, collapsed: Boolean) {
        when (panel) {
            MapPanel.Card -> memory.cardCollapsed = collapsed
            MapPanel.Legend -> memory.legendCollapsed = collapsed
            MapPanel.Tally -> memory.tallyCollapsed = collapsed
        }
    }
}
