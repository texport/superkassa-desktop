package kz.mybrain.superkassa.desktop.ui.map

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.AddressSuggestion
import kz.mybrain.superkassa.desktop.server.cabinet.RegisterAddress
import kz.mybrain.superkassa.desktop.server.cabinet.resolveAddress
import kz.mybrain.superkassa.desktop.ui.strings.MapAddressTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Подбор адреса по метке на карте.
 *
 * Владелец ставит метку там, где стоит его магазин, а служба карт говорит,
 * что это за место. Адресом точки названное службой не становится: РКА
 * и САТО есть только у государственного регистра, поэтому найденное место
 * проходит те же шаги регистра, что и каскад, и владельцу предлагаются
 * записи регистра — из них он и выбирает.
 *
 * Молчания нет ни в одном исходе: не ответила служба, не знает места,
 * не нашлось области, пункта, улицы или дома — обо всём сказано словами,
 * и каскад остаётся на месте как второй путь.
 *
 * @param onChosen выбранный владельцем дом, подтверждённый регистром.
 */
@Composable
internal fun PointAddress(
    cabinet: CabinetSession,
    state: MapState,
    reverse: MapReverseGeocoder,
    notices: MapAddressTexts,
    onChosen: (RegisterAddress) -> Unit
) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var match by remember { mutableStateOf<PointMatch?>(null) }
    // Метку владелец мог передвинуть — прежний подбор относится уже
    // не к ней, и его дома предлагали бы адрес другого места.
    LaunchedEffect(state.markerLatitude, state.markerLongitude) { match = null }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        PickRow(notices.byPoint, state.marked && !busy, pointNotice(state.marked, busy, match, notices)) {
            scope.launch {
                busy = true
                match = placeUnder(cabinet, state, reverse)
                busy = false
            }
        }
        val found = match as? PointMatch.Houses ?: return@Column
        HouseChoice(cabinet, found.items) { chosen ->
            match = null
            onChosen(chosen)
        }
    }
}

/** Кнопка подбора и строка о его ходе: действие и ответ на него рядом. */
@Composable
private fun PickRow(label: String, enabled: Boolean, notice: String?, onPick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(enabled = enabled, onClick = onPick) { Text(label) }
        if (notice != null) {
            Text(
                text = notice,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Найденные дома — плашками с кодом РКА.
 *
 * Код показан по той же причине, что и в шаге дома: на один номер регистр
 * отдаёт две записи с разными РКА, и различаются они только кодом.
 */
@Composable
private fun HouseChoice(
    cabinet: CabinetSession,
    houses: List<AddressSuggestion>,
    onChosen: (RegisterAddress) -> Unit
) {
    val scope = rememberCoroutineScope()
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        houses.forEach { house ->
            SuggestionChip(
                onClick = { scope.launch { resolved(cabinet, house)?.let(onChosen) } },
                label = {
                    Text(
                        text = listOfNotNull(house.name, house.rka).joinToString(Glyphs.SEPARATOR),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

/**
 * Место под меткой и отвечающие ему дома регистра.
 *
 * Обе службы отвечают не всегда, и ждать их окно не должно: за общий срок
 * подбор кончается сам, а владелец видит, что место не узнано, и уходит
 * в каскад.
 */
private suspend fun placeUnder(cabinet: CabinetSession, state: MapState, reverse: MapReverseGeocoder): PointMatch {
    val latitude = state.markerLatitude ?: return PointMatch.Missing(PointStep.Place)
    val longitude = state.markerLongitude ?: return PointMatch.Missing(PointStep.Place)
    val token = cabinet.token ?: return PointMatch.Missing(PointStep.Place)
    val found = withTimeoutOrNull(PICK_TIMEOUT_MS) {
        val place = reverse.at(latitude, longitude) ?: return@withTimeoutOrNull null
        matchPointPlace(CabinetRegistrySteps(cabinet, token), place)
    }
    return found ?: PointMatch.Missing(PointStep.Place)
}

/** Адрес регистра по коду РКА выбранного дома: то, что уходит в точку. */
private suspend fun resolved(cabinet: CabinetSession, house: AddressSuggestion): RegisterAddress? {
    val rka = house.rka ?: return null
    val token = cabinet.token ?: return null
    return cabinet.guard { cabinet.client.resolveAddress(token, rka) }
}

/**
 * Что сказать владельцу о ходе подбора; `null` — сказать пока нечего.
 *
 * Пока метки нет, кнопка погашена, и молчать при этом нельзя: погашенная
 * кнопка без причины читается как неработающая. Сказано ровно то, чего
 * не хватает, — метки на карте.
 */
private fun pointNotice(
    marked: Boolean,
    busy: Boolean,
    match: PointMatch?,
    notices: MapAddressTexts
): String? = when {
    busy -> notices.byPointSearching
    !marked -> notices.markFirst
    match is PointMatch.Houses -> notices.byPointHouses
    match is PointMatch.Missing -> when (match.step) {
        PointStep.Place -> notices.byPointNoPlace
        PointStep.Region -> notices.byPointNoRegion
        PointStep.Locality -> notices.byPointNoLocality
        PointStep.Street -> notices.byPointNoStreet
        PointStep.House -> notices.byPointNoHouse
    }

    else -> null
}

/** Сколько всего ждать подбор: службы карт отвечают не всегда. */
private const val PICK_TIMEOUT_MS = 20_000L
