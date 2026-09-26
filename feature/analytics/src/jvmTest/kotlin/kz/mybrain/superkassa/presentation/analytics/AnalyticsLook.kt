package kz.mybrain.superkassa.presentation.analytics

import kz.mybrain.superkassa.Look
import kz.mybrain.superkassa.MapScene
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsKkm
import kz.mybrain.superkassa.domain.analytics.model.KkmMapView
import kz.mybrain.superkassa.domain.analytics.model.PlacedKkm
import kz.mybrain.superkassa.presentation.analytics.map.AnalyticsMapUiState
import kz.mybrain.superkassa.presentation.analytics.map.FIT_HEIGHT
import kz.mybrain.superkassa.presentation.analytics.map.FIT_WIDTH
import kz.mybrain.superkassa.presentation.analytics.map.MapTools
import kz.mybrain.superkassa.presentation.analytics.map.MapWords
import kz.mybrain.superkassa.presentation.analytics.map.fitting
import kz.mybrain.superkassa.presentation.common.mapview.MapCases
import kz.mybrain.superkassa.presentation.common.mapview.MapFold
import kz.mybrain.superkassa.presentation.common.mapview.MapTiles
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Оснастка снимков аналитики: одни и те же кассы, надписи и средства карты
 * во всех наборах.
 *
 * Одна касса в одном наборе не должна оказаться с номером КГД, а в другом
 * без него: разница на картинке будет не та, которую проверяют. Плитки
 * берутся с заведомо недоступного источника — ни одного обращения в сеть.
 */
internal object AnalyticsLook {

    val texts = textsOf(Language.Ru).analytics
    val cabinet = Look.cabinet
    val words = MapWords(texts, cabinet)

    /** Середина Алматы: с неё начинается карта приложения. */
    const val LATITUDE = MapScene.LATITUDE
    const val LONGITUDE = MapScene.LONGITUDE

    /** Увеличение, на котором виден город. */
    const val CITY_ZOOM = 12

    /** Настолько в стороне, что в один ярлычок с соседом не сойдётся. */
    const val APART = 0.01

    /** Сценарии карты без сети и со своей памятью: чужие настройки не трогать. */
    fun mapCases(): MapCases = MapScene.cases()

    /** Плитки, которых не будет: ни сети, ни чужого кэша. */
    fun tiles(): MapTiles = MapScene.tiles()

    /** Состояние карты касс, с которого начинается раздел. */
    fun model(): AnalyticsMapUiState = AnalyticsMapUiState()

    /** Карта ведётся к кассам тем же правилом, что и в модели раздела. */
    fun centre(model: AnalyticsMapUiState, placed: List<PlacedKkm>) {
        fitting(placed, FIT_WIDTH, FIT_HEIGHT)?.let { model.map.centreOn(it.latitude, it.longitude, it.zoom) }
    }

    /** Карточка под картой развёрнута, как у нового рабочего места; своя память — чужие настройки не трогать. */
    fun panel(): MapFold = mapCases().card()

    /** Легенда карты — так же развёрнута и так же со своей памятью. */
    fun legend(): MapFold = mapCases().legend()

    /** Средства карты касс без сети. */
    fun tools(panel: MapFold = panel()): MapTools {
        val map = mapCases()
        return MapTools(map.tiles(), map.locating(), panel, map.legend(), map.tally())
    }

    /** Касса аналитики: одна и та же во всех наборах снимков. */
    fun kkm(
        at: Int,
        place: String? = "Магазин на Абая",
        placeId: String? = "p-1",
        address: String? = "г. Алматы, пр. Абая, 10",
        blocked: Boolean = false,
        status: String? = "REGISTERED",
        shiftOpen: Boolean = false
    ) = AnalyticsKkm(
        cashRegisterId = "c$at",
        kkmId = 2000300 + at,
        registrationNumber = "%012d".format(4500000L + at),
        internalName = "Касса $at",
        retailPlaceId = placeId,
        retailPlaceName = place,
        address = address,
        status = status,
        blocked = blocked,
        shiftStatus = if (shiftOpen) "OPEN" else "CLOSED",
        shiftNumber = at.toLong(),
        lastContactAt = "2026-09-20T19:47:00Z"
    )

    fun view(placed: List<AnalyticsKkm>, without: List<AnalyticsKkm> = emptyList()) = KkmMapView(
        placedCount = placed.size,
        withoutPositionCount = without.size,
        placed = placed,
        withoutPosition = without
    )
}
