package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.placesCollapsed
import kz.mybrain.superkassa.desktop.app.togglePlaces
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Хозяйство владельца так, как оно устроено: точка — кассы — документы.
 *
 * Прежде это были три раздела подряд: точки, кассы, документы. Владелец
 * выбирал точку в одном, ту же кассу во втором и её же в третьем — а из
 * списка касс было не видно, какая где стоит. Теперь список один: точки,
 * под каждой её кассы, а выбранная касса раскрывается справа вместе
 * со своими документами.
 *
 * Список сворачивается, как рельс разделов: когда работают с одной кассой,
 * её карточке нужна вся ширина окна, а свёрнутая колонка остаётся рельсом
 * значков. Выбор помнится рабочим местом.
 *
 * Поиск сужает обе части списка сразу: у сети бывают сотни точек, и найти
 * среди них кассу глазами нельзя.
 */
@Composable
fun PlacesPage(session: Session, cabinet: CabinetSession, texts: CabinetTexts) {
    val scope = rememberCoroutineScope()
    val places = cabinet.places
    var place by remember { mutableStateOf<String?>(null) }
    var register by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    // Первого ответа кабинета ещё не было: пустая колонка до него читалась
    // как «точек нет», хотя их просто ещё не спросили.
    var answered by remember(cabinet.token) { mutableStateOf(false) }
    // Отказ по списку точек держится здесь, а не берётся у сеанса: помеху
    // сеанса каркас окна забирает во всплывающую строку и тут же гасит,
    // а колонка обязана называть причину, пока список не прочитан.
    var trouble by remember(cabinet.token) { mutableStateOf<String?>(null) }

    suspend fun reload() {
        val read = cabinet.refreshPlaces()
        trouble = if (read) null else cabinet.problem?.let { cabinetMessage(it, texts).words() } ?: texts.unreachable
        cabinet.refreshRegisters()
        answered = true
    }

    LaunchedEffect(cabinet.token) { reload() }

    val refresh = { scope.launch { reload() } }
    Row(modifier = Modifier.fillMaxSize()) {
        PlaceTree(
            texts = texts,
            language = session.language,
            collapsed = session.placesCollapsed,
            onToggle = { session.togglePlaces() },
            rows = placeRows(places, cabinet.registers, place, query),
            // Сколько точек у компании — по словам кабинета: пока список
            // дочитывается, прочитано меньше, и колонка об этом говорит.
            total = maxOf(places.size, cabinet.placesTotal),
            // Строки встают, как только пришла первая страница: у сети их
            // сорок, и ожидание до последней заняло бы весь показ.
            loading = !answered && places.isEmpty(),
            // Почему список пуст: кабинет отказал или у владельца и правда
            // нет ни одной точки. Слова — те же, какими отказал кабинет.
            trouble = trouble,
            onRetry = { refresh() },
            query = query,
            onQuery = { query = it },
            place = place,
            register = register,
            onPlace = {
                place = it
                register = null
            },
            onRegister = { register = it },
            footer = { PlaceCreateButtons(session, cabinet, texts, place) { refresh() } }
        )
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Detail(session, cabinet, texts, places, place, register) { refresh() }
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
        chosen != null -> RegisterDetails(session, cabinet, texts, chosen, modifier = pane)
        chosenPlace != null -> PlaceCard(session, cabinet, texts, chosenPlace, pane, onChanged)
        else -> EmptyState(
            icon = AppIcons.newKkm,
            title = texts.pickRegisterFirst,
            hint = texts.hints.pickRegisterFirst,
            modifier = pane
        )
    }
}
