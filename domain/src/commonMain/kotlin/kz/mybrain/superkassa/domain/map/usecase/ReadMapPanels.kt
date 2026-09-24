package kz.mybrain.superkassa.domain.map.usecase

import kz.mybrain.superkassa.domain.map.model.MapPanels
import kz.mybrain.superkassa.domain.map.port.MapMemory

/** Что владелец свернул на карте касс в прошлый раз. */
class ReadMapPanels(private val memory: MapMemory) {

    operator fun invoke(): MapPanels = MapPanels(memory.cardCollapsed, memory.legendCollapsed)
}
