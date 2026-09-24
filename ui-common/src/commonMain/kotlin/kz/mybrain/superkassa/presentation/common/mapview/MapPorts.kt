package kz.mybrain.superkassa.presentation.common.mapview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kz.mybrain.superkassa.domain.map.model.MapPanel
import kz.mybrain.superkassa.domain.map.port.MapMemory
import kz.mybrain.superkassa.domain.map.port.Maps
import kz.mybrain.superkassa.domain.map.usecase.AnswerLocationAsk
import kz.mybrain.superkassa.domain.map.usecase.FindAddress
import kz.mybrain.superkassa.domain.map.usecase.FindHouses
import kz.mybrain.superkassa.domain.map.usecase.FoldMapPanel
import kz.mybrain.superkassa.domain.map.usecase.LocateSelf
import kz.mybrain.superkassa.domain.map.usecase.NamePoint
import kz.mybrain.superkassa.domain.map.usecase.ReadMapPanels
import kz.mybrain.superkassa.domain.map.usecase.ReadTile

/**
 * Службы карты и память о ней — одним набором.
 *
 * Карта нужна нескольким областям: аналитике — картой касс, кабинету —
 * выбором места торговой точки. Набор и сценарии поэтому лежат на общей
 * полке, а не в одной из областей.
 *
 * @property maps плитки, поиск адреса и своё место.
 * @property memory что рабочее место помнит о карте.
 */
class MapPorts(val maps: Maps, val memory: MapMemory) {

    /** Сценарии карты на этих портах. */
    fun cases(): MapCases = MapCases(this)
}

/** Сценарии карты: плитки, поиск, место под меткой, своё место и свёрнутые части. */
class MapCases(ports: MapPorts) {
    val readTile = ReadTile(ports.maps)
    val findAddress = FindAddress(ports.maps)
    val findHouses = FindHouses(ports.maps)
    val namePoint = NamePoint(ports.maps)
    val locateSelf = LocateSelf(ports.maps, ports.memory)
    val answerAsk = AnswerLocationAsk(ports.maps, ports.memory)
    val readPanels = ReadMapPanels(ports.memory)
    val foldPanel = FoldMapPanel(ports.memory)

    /** Плитки на время одной карты. */
    fun tiles(): MapTiles = MapTiles(readTile)

    /** Кнопка «Где я» на время одной карты. */
    fun locating(): MapLocating = MapLocating(locateSelf, answerAsk)

    /** Карточка под картой касс: свёрнута ли она, помнит рабочее место. */
    fun card(): MapFold = MapFold(MapPanel.Card, !readPanels().cardCollapsed, foldPanel)

    /** Легенда карты касс: свёрнута ли она, помнит рабочее место. */
    fun legend(): MapFold = MapFold(MapPanel.Legend, !readPanels().legendCollapsed, foldPanel)
}

/**
 * Развёрнута ли часть карты — карточка под ней или легенда в углу.
 *
 * Высота раздела одна на карту и её части, и распоряжается ею владелец:
 * ищет кассу глазами — сворачивает, читает — разворачивает. Выбор помнится
 * рабочим местом: повторять нажатие при каждом открытии раздела владелец
 * не должен.
 *
 * @param expanded развёрнута ли часть при открытии карты.
 */
class MapFold(private val panel: MapPanel, expanded: Boolean, private val fold: FoldMapPanel) {

    var expanded: Boolean by mutableStateOf(expanded)
        private set

    /** Сворачивает развёрнутое и наоборот; выбор запоминается. */
    fun toggle() {
        expanded = !expanded
        fold(panel, collapsed = !expanded)
    }
}
