package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.ui.cabinet.PlaceCard
import kz.mybrain.superkassa.desktop.ui.cabinet.PlaceCreateButtons
import kz.mybrain.superkassa.desktop.ui.cabinet.PlaceTree
import kz.mybrain.superkassa.desktop.ui.cabinet.placeRows
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

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
        latitude = if (point) BigDecimal("43.238949") else null,
        longitude = if (point) BigDecimal("76.889709") else null,
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
        retailPlaceId = place
    )
}

/**
 * Раздел торговых точек так, как его собирает `PlacesPage`.
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
    query: String = "",
    loading: Boolean = false,
    collapsed: Boolean = false
) {
    val session = Look.session()
    val cabinet = CabinetSession()
    Row(modifier = Modifier.fillMaxSize()) {
        PlaceTree(
            texts = Look.cabinet,
            language = Language.Ru,
            collapsed = collapsed,
            onToggle = {},
            rows = placeRows(places, registers, open, query),
            total = places.size,
            loading = loading,
            trouble = null,
            onRetry = {},
            query = query,
            onQuery = {},
            place = open,
            register = null,
            onPlace = {},
            onRegister = {},
            footer = { PlaceCreateButtons(session, cabinet, Look.cabinet, open) {} }
        )
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        val chosen = places.firstOrNull { it.id == open }
        val pane = Modifier.weight(1f).padding(start = Spacing.screen)
        if (chosen == null) {
            EmptyState(
                icon = AppIcons.newKkm,
                title = Look.cabinet.pickRegisterFirst,
                hint = Look.cabinet.hints.pickRegisterFirst,
                modifier = pane
            )
        } else {
            PlaceCard(session, cabinet, Look.cabinet, chosen, pane) {}
        }
    }
}
