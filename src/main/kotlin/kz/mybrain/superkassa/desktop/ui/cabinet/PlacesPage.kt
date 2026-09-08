package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.server.cabinet.removeRetailPlace
import kz.mybrain.superkassa.desktop.server.cabinet.retailPlaces
import kz.mybrain.superkassa.desktop.ui.components.Collapsible
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.components.RecordRow
import kz.mybrain.superkassa.desktop.ui.components.ScrollableColumn
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Торговые точки компании.
 *
 * Адрес не набирается руками, а выбирается из регистра: в заявление уходит
 * его код (РКА), и произвольная строка там не пройдёт. Поэтому поле адреса
 * — поиск по регистру, а не свободный ввод.
 */
@Composable
fun PlacesPage(session: Session, cabinet: CabinetSession, texts: CabinetTexts) {
    val scope = rememberCoroutineScope()
    val places = remember { mutableStateListOf<RetailPlace>() }
    var opened by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        val token = cabinet.token ?: return
        cabinet.guard { cabinet.client.retailPlaces(token) }?.let { page ->
            places.clear()
            places.addAll(page.items)
        }
    }

    fun remove(place: RetailPlace) = scope.launch {
        val token = cabinet.token ?: return@launch
        if (cabinet.guard { cabinet.client.removeRetailPlace(token, place.id) } != null) reload()
    }

    LaunchedEffect(cabinet.token) { reload() }

    ScrollableColumn(modifier = Modifier.fillMaxWidth(), spacing = Spacing.snug) {
        SectionCard(title = texts.places, count = places.size.toString()) {
            if (places.isEmpty()) {
                EmptyState(AppIcons.newKkm, texts.placesEmpty, texts.placesEmptyHint)
            }
            places.forEachIndexed { at, place ->
                // Правка раскрывается по нажатию на строку: список из десятка
                // точек с развёрнутыми формами не читается, а переименовывают
                // и переезжают редко.
                val open = place.id == opened
                PlaceRow(session.language, place, texts, at % STRIPE == 1, { remove(place) }) {
                    opened = if (open) null else place.id
                }
                Collapsible(open) {
                    PlaceEditRow(session, cabinet, texts, place) { scope.launch { reload() } }
                }
            }
        }
        AddPlaceCard(session, cabinet, texts) { scope.launch { reload() } }
    }
}

/** Строка точки: название, адрес под ним и сколько касс к ней привязано. */
@Composable
private fun PlaceRow(
    language: Language,
    place: RetailPlace,
    texts: CabinetTexts,
    striped: Boolean,
    onRemove: () -> Unit,
    onOpen: () -> Unit
) {
    RecordRow(
        title = place.name,
        subtitle = addressIn(language, place.address, place.addressKz),
        striped = striped,
        onClick = onOpen,
        trailing = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${texts.registerCount}: ${place.cashRegisterCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                PlaceRemove(place, texts, onRemove)
            }
        }
    )
}

/**
 * Удаление точки — или объяснение, почему его нет.
 *
 * Погашенная кнопка молчит о причине: владелец видел, что точку удалить
 * нельзя, но не знал, что мешает именно привязанная касса. Вместо неё
 * стоит объяснение под значком.
 */
@Composable
private fun PlaceRemove(place: RetailPlace, texts: CabinetTexts, onRemove: () -> Unit) {
    if (place.cashRegisterCount > 0) {
        InfoTip(texts.placeRemoveBlocked)
        return
    }
    IconButton(onClick = onRemove) {
        Icon(AppIcons.close, contentDescription = texts.remove)
    }
}

/** Затеняется каждая вторая строка списка. */
private const val STRIPE = 2
