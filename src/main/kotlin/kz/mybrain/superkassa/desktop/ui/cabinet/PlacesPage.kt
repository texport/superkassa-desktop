package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.server.cabinet.retailPlaces
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.components.RecordRow
import kz.mybrain.superkassa.desktop.ui.components.ScrollableColumn
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Хозяйство владельца так, как оно устроено: точка — кассы — документы.
 *
 * Прежде это были три раздела подряд: точки, кассы, документы. Владелец
 * выбирал точку в одном, ту же кассу во втором и её же в третьем — а из
 * списка касс было не видно, какая где стоит. Теперь список один: точки,
 * под каждой её кассы, а выбранная касса раскрывается справа вместе
 * со своими документами.
 */
@Composable
fun PlacesPage(session: Session, cabinet: CabinetSession, texts: CabinetTexts) {
    val scope = rememberCoroutineScope()
    val places = remember { mutableStateListOf<RetailPlace>() }
    var place by remember { mutableStateOf<String?>(null) }
    var register by remember { mutableStateOf<String?>(null) }

    suspend fun reload() {
        val token = cabinet.token ?: return
        cabinet.guard { cabinet.client.retailPlaces(token) }?.let { page ->
            places.clear()
            places.addAll(page.items)
        }
        cabinet.refreshRegisters()
    }

    LaunchedEffect(cabinet.token) { reload() }

    Row(modifier = Modifier.fillMaxSize()) {
        PlaceTree(
            session = session,
            cabinet = cabinet,
            texts = texts,
            places = places,
            place = place,
            register = register,
            onPlace = {
                place = it
                register = null
            },
            onRegister = { register = it },
            onChanged = { scope.launch { reload() } }
        )
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Detail(session, cabinet, texts, places, place, register) { scope.launch { reload() } }
    }
}

/** Дерево слева: точки и их кассы, под ними — создание того и другого. */
@Composable
private fun PlaceTree(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    places: List<RetailPlace>,
    place: String?,
    register: String?,
    onPlace: (String) -> Unit,
    onRegister: (String) -> Unit,
    onChanged: () -> Unit
) {
    Column(
        modifier = Modifier.width(Sizes.registerColumn).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        ScrollableColumn(modifier = Modifier.weight(1f), spacing = Spacing.tight, gutter = Spacing.screen) {
            SectionCard(title = texts.places) {
                if (places.isEmpty()) {
                    EmptyState(AppIcons.newKkm, texts.placesEmpty, texts.placesEmptyHint)
                }
                places.forEach { item ->
                    PlaceBranch(cabinet, texts, item, place, register, onPlace, onRegister)
                }
            }
        }
        CreateButtons(session, cabinet, texts, place, onChanged)
    }
}

/** Точка и её кассы под ней. Свёрнутая точка показывает только себя. */
@Composable
private fun PlaceBranch(
    cabinet: CabinetSession,
    texts: CabinetTexts,
    item: RetailPlace,
    place: String?,
    register: String?,
    onPlace: (String) -> Unit,
    onRegister: (String) -> Unit
) {
    val open = item.id == place
    RecordRow(
        title = item.name,
        subtitle = "${texts.registerCount}: ${item.cashRegisterCount}",
        selected = open && register == null,
        onClick = { onPlace(item.id) }
    )
    if (!open) return
    cabinet.registers.filter { it.retailPlace?.id == item.id }.forEach { kkm ->
        RecordRow(
            title = registerTitle(kkm),
            subtitle = kkm.registrationNumber,
            selected = kkm.id == register,
            modifier = Modifier.padding(start = Spacing.normal),
            onClick = { onRegister(kkm.id) },
            trailing = { CabinetStatusChip(kkm.status, texts) }
        )
    }
}

/**
 * Создание точки и кассы — окнами.
 *
 * Кнопки отступают от правого края на то же поле, что и список над ними:
 * иначе кнопка шире карточки ровно на ширину полосы прокрутки, и края
 * не сходятся.
 */
@Composable
private fun CreateButtons(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    place: String?,
    onChanged: () -> Unit
) {
    var addingPlace by remember { mutableStateOf(false) }
    var addingRegister by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(end = Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        FilledTonalButton(onClick = { addingPlace = true }, modifier = Modifier.fillMaxWidth()) {
            Text(texts.addPlace)
        }
        FilledTonalButton(
            enabled = place != null,
            onClick = { addingRegister = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text(texts.addRegister) }
    }
    if (addingPlace) {
        AddPlaceCard(session, cabinet, texts, onDismiss = { addingPlace = false }, onAdded = onChanged)
    }
    if (addingRegister) {
        AddRegisterDialog(session, cabinet, texts, onDismiss = { addingRegister = false }) { onChanged() }
    }
}

/**
 * Справа — выбранная касса целиком, а до выбора кассы сама точка.
 *
 * Отступ от разделителя такой же, какой слева даёт поле под полосу
 * прокрутки: карточки справа начинались вплотную к черте, и колонки
 * выглядели прижатыми друг к другу.
 */
@Composable
private fun RowScope.Detail(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    places: List<RetailPlace>,
    place: String?,
    register: String?,
    onChanged: () -> Unit
) {
    val chosen = cabinet.registers.firstOrNull { it.id == register }
    val chosenPlace = places.firstOrNull { it.id == place }
    val pane = Modifier.weight(1f).padding(start = Spacing.screen)
    when {
        chosen != null -> RegisterDetails(cabinet, texts, chosen, modifier = pane)
        chosenPlace != null -> PlaceCard(session, cabinet, texts, chosenPlace, pane, onChanged)
        else -> EmptyState(
            icon = AppIcons.newKkm,
            title = texts.pickRegisterFirst,
            hint = texts.pickRegisterFirstHint,
            modifier = pane
        )
    }
}
