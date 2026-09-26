package kz.mybrain.superkassa.presentation.cabinet.places

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.Look
import kz.mybrain.superkassa.Windowed
import kz.mybrain.superkassa.designsystem.state.EmptyState
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.idleCabinet
import kz.mybrain.superkassa.presentation.cabinet.places.component.PlaceCard
import kz.mybrain.superkassa.presentation.cabinet.places.component.PlaceCreateButtons
import kz.mybrain.superkassa.presentation.cabinet.places.component.PlaceTree
import kz.mybrain.superkassa.presentation.cabinet.places.component.placeRows
import kz.mybrain.superkassa.strings.api.Language

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
                onCollapse = {},
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
                    title = Look.cabinet.places.pickRegisterFirst,
                    hint = Look.cabinet.hints.pickRegisterFirst,
                    modifier = pane
                )
            } else {
                PlaceCard(cabinet, Look.cabinet, chosen, pane)
            }
        }
    }
}
