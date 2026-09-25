package kz.mybrain.superkassa

import kz.mybrain.superkassa.domain.map.MemoryMapMemory
import kz.mybrain.superkassa.domain.map.QuietMaps
import kz.mybrain.superkassa.presentation.common.mapview.MapCases
import kz.mybrain.superkassa.presentation.common.mapview.MapPorts
import kz.mybrain.superkassa.presentation.common.mapview.MapTiles

/**
 * Карта для проверок: службы без сети, память своя у каждой проверки.
 *
 * Карту рисуют и общее экранов, и аналитика, и выбор места точки —
 * одна сцена на всех, чтобы проверки разных модулей видели один город.
 */
object MapScene {

    /** Середина Алматы: с неё начинается карта приложения. */
    const val LATITUDE = 43.238949
    const val LONGITUDE = 76.889709

    /** Службы карты без сети и со своей памятью: чужие настройки не трогать. */
    fun ports(): MapPorts = MapPorts(QuietMaps(), MemoryMapMemory())

    /** Сценарии карты на [ports]. */
    fun cases(): MapCases = ports().cases()

    /** Плитки, которых не будет: ни сети, ни чужого кэша. */
    fun tiles(): MapTiles = cases().tiles()
}
