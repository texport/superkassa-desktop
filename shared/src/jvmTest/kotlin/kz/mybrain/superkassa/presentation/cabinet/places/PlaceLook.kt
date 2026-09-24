package kz.mybrain.superkassa.presentation.cabinet.places

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import kz.mybrain.superkassa.Look
import kz.mybrain.superkassa.Windowed
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlaceRef
import kz.mybrain.superkassa.idleCabinet
import kz.mybrain.superkassa.presentation.cabinet.places.component.PlaceCard
import kz.mybrain.superkassa.presentation.cabinet.places.component.PlaceCreateButtons
import kz.mybrain.superkassa.presentation.cabinet.places.component.PlaceTree
import kz.mybrain.superkassa.presentation.cabinet.places.component.placeRows
import kz.mybrain.superkassa.presentation.common.state.EmptyState
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Составы раздела торговых точек для снимков.
 *
 * Точка бывает без адреса, без координат и с десятком касс под собой,
 * и все три состояния нужны сразу нескольким снимкам: собранные в каждом
 * заново, они разошлись бы мелочами.
 */
internal object PlaceLook {

    /** Точка с адресом и координатами — обычная, какой её заводят. */
    fun place(
        at: Int,
        address: String? = "г. Алматы, пр. Абая, $at",
        point: Boolean = true,
        registers: Long = 2
    ) = RetailPlace(
        id = "p$at",
        name = "Магазин на Абая $at",
        addressRef = address?.let { "RKA-$at" },
        rka = address?.let { "%010d".format(at.toLong()) },
        cato = "751310000",
        address = address,
        addressKz = address?.let { "Алматы қ., Абай даң., $at" },
        latitude = if (point) Decimal.parse("43.238949") else null,
        longitude = if (point) Decimal.parse("76.889709") else null,
        cashRegisterCount = registers
    )

    /** Касса под точкой: на учёте или снятая с него. */
    fun register(at: Int, place: String, onRecord: Boolean = true) = CabinetRegister(
        id = "r$at",
        kkmId = 2000300 + at,
        internalName = "Касса $at",
        status = if (onRecord) "REGISTERED" else "DEREGISTERED",
        registrationNumber = "%012d".format(4500000L + at),
        factoryNumber = "SK-$at",
        retailPlace = RetailPlaceRef(place)
    )
}

/**
 * Раздел торговых точек так, как его собирает `PlacesScreen`.
 *
 * Сама страница спрашивает кабинет, и подставить ей «точек нет вовсе»
 * снаружи нельзя. Здесь те же два столбца с той же чертой между ними:
 * на снимке видно ровно то, что увидит владелец.
 */
@Composable
internal fun PlacesLook(
    places: List<RetailPlace>,
    registers: List<CabinetRegister>,
    open: String? = null,
    sieve: PlaceSieve = PlaceSieve(),
    loading: Boolean = false,
    collapsed: Boolean = false,
    /** Кассы, заблокированные по словам кабинета, и знает ли он о них. */
    locked: Set<String> = emptySet(),
    locksKnown: Boolean = true,
    /** Кабинет списка не отдал — его словами; `null` — отдал. */
    trouble: String? = null
) {
    val cabinet = remember { idleCabinet() }
    // Карточка точки и кнопки заведения берут свои модели у окна.
    Windowed {
        Row(modifier = Modifier.fillMaxSize()) {
            PlaceTree(
                texts = Look.cabinet,
                language = Language.Ru,
                collapsed = collapsed,
                onToggle = {},
                rows = placeRows(places, registers, open, sieve, locked, Language.Ru),
                total = places.size,
                loading = loading,
                trouble = trouble,
                onRetry = {},
                sieve = sieve,
                onSieve = {},
                locksKnown = locksKnown,
                place = open,
                register = null,
                onPlace = {},
                onRegister = {},
                footer = { PlaceCreateButtons(cabinet, Look.cabinet, open) }
            )
            VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            val chosen = places.firstOrNull { it.id == open }
            val pane = Modifier.weight(1f).padding(start = Spacing.fieldGap)
            if (chosen == null) {
                EmptyState(
                    icon = AppIcons.newKkm,
                    title = Look.cabinet.pickRegisterFirst,
                    hint = Look.cabinet.hints.pickRegisterFirst,
                    modifier = pane
                )
            } else {
                PlaceCard(cabinet, Look.cabinet, chosen, pane)
            }
        }
    }
}
