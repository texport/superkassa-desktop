package kz.mybrain.superkassa.presentation.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.Look
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.cabinet.model.RegisterAddress
import kz.mybrain.superkassa.idleCabinet
import kz.mybrain.superkassa.presentation.analytics.AnalyticsLook
import kz.mybrain.superkassa.presentation.cabinet.places.component.CabinetRegistry
import kz.mybrain.superkassa.presentation.common.mapview.CITY_ZOOM
import kz.mybrain.superkassa.presentation.common.mapview.HOUSE_ZOOM
import kz.mybrain.superkassa.presentation.common.mapview.MapState
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test

/**
 * Снимки окна выбора места на карте.
 *
 * Плитки не приходят намеренно: у владельца без связи окно обязано
 * оставаться рабочим — метка ставится из проекции, а не из картинки, —
 * и на снимке видно, говорит ли об этом экран хоть что-нибудь.
 */
class PlaceMapShots {

    /** Плиток нет и метка не поставлена: с чего окно начинается без связи. */
    @Test
    fun `плитки не пришли и метка не поставлена`() = look("place-map-no-tiles") { MapState() }

    /** Метка поставлена: координаты в подвале и кнопка выбора доступна. */
    @Test
    fun `метка поставлена`() = look("place-map-marked") {
        MapState().also { it.mark(AnalyticsLook.LATITUDE, AnalyticsLook.LONGITUDE) }
    }

    /** Координаты введены руками: карта встала на них, метка та же. */
    @Test
    fun `координаты введены руками`() = look("place-map-by-degrees") {
        MapState().also {
            it.show(AnalyticsLook.LATITUDE, AnalyticsLook.LONGITUDE, HOUSE_ZOOM)
            it.mark(AnalyticsLook.LATITUDE, AnalyticsLook.LONGITUDE)
        }
    }

    /** Своё место определено лишь городом: об этом сказано под координатами. */
    @Test
    fun `своё место известно лишь городом`() = look("place-map-city-only") {
        MapState().also {
            it.showLocation(AnalyticsLook.LATITUDE, AnalyticsLook.LONGITUDE, city = "Алматы", toZoom = CITY_ZOOM)
            it.mark(AnalyticsLook.LATITUDE, AnalyticsLook.LONGITUDE)
        }
    }

    /** Окно у готовой точки: адрес уже выбран в регистре. */
    @Test
    fun `адрес точки уже выбран`() = look("place-map-with-address", address = HOUSE) {
        MapState().also { it.mark(AnalyticsLook.LATITUDE, AnalyticsLook.LONGITUDE) }
    }

    /**
     * Снимок окна карты.
     *
     * Ряды те же и в том же порядке, что собирает `MapPickerDialog`,
     * и та же заданная высота: окно строится из них, а не из своей
     * разметки. Плитки свои — чужого кэша окно не увидит.
     */
    private fun look(name: String, address: RegisterAddress? = null, state: () -> MapState) {
        RenderProbe(WIDE, HIGH) { PickerLook(address, remember { state() }) }
            .use { probe ->
                repeat(SETTLE) { probe.frame() }
                Look.shot(name, probe.frame())
            }
    }

    private companion object {
        val HOUSE = RegisterAddress(
            addressRef = "RKA-1",
            address = "г. Алматы, пр. Абая, 10",
            addressKz = "Алматы қ., Абай даң., 10",
            rka = "0000000001",
            cato = "751310000"
        )

        const val HOUSE_ZOOM = 17
        const val CITY_ZOOM = 12
        const val SETTLE = 24
        const val WIDE = 1180
        const val HIGH = 820
    }
}

/** Окно выбора места так, как его собирает `MapPickerDialog`. */
@Composable
private fun PickerLook(address: RegisterAddress?, state: MapState) {
    val registry = remember { CabinetRegistry(idleCabinet()) }
    val notices = textsOf(Language.Ru).map.address
    val cases = remember { AnalyticsLook.mapCases() }
    val parts = remember {
        val pick = MapAddressPick(address, Language.Ru)
        MapPickerParts(cases, cases.tiles(), cases.locating(), registry, pick, state)
    }
    Surface(
        modifier = Modifier.width(Sizes.mapWidth).height(Sizes.mapDialogHeight),
        shape = RoundedCornerShape(Sizes.corner),
        tonalElevation = Sizes.dialogElevation
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
        ) {
            MapHeader(Look.cabinet) {}
            RegistryAddress(parts, address) {}
            MapArea(state, parts.tiles, Look.cabinet, parts.locating, Modifier.weight(1f))
            PointAddress(registry, state, cases.namePoint, notices) {}
            DegreesEntry(state, Look.cabinet)
            MapFooter(state, Look.cabinet, {}) {}
        }
    }
}
