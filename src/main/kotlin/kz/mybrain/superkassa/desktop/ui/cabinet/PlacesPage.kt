package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import kz.mybrain.superkassa.desktop.ui.adaptive.NarrowPanes
import kz.mybrain.superkassa.desktop.ui.adaptive.TwoPane
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.CabinetPanes

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
 * Колонка и карточка делят место долями [CabinetPanes.placesAndReadingCard],
 * а не постоянной шириной колонки: на планшете стоймя колонка в четыреста
 * точек оставляла карточке столбик в букву шириной. Где обеим тесно,
 * карточка сменяет колонку целиком.
 *
 * Поиск сужает обе части списка сразу: у сети бывают сотни точек, и найти
 * среди них кассу глазами нельзя. Отбор и порядок — там же: пять тысяч
 * касс, из которых на учёте единицы, поиском по названию не перебрать.
 */
@Composable
fun PlacesPage(session: Session, cabinet: CabinetSession, texts: CabinetTexts) {
    val scope = rememberCoroutineScope()
    val places = cabinet.places
    var place by remember { mutableStateOf<String?>(null) }
    var register by remember { mutableStateOf<String?>(null) }
    var sieve by remember { mutableStateOf(PlaceSieve()) }
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
        // Блокировки — последними: список точек и касс уже на экране,
        // а сводка по блокировкам нужна только плашке отбора.
        cabinet.refreshBlocked()
    }

    LaunchedEffect(cabinet.token) { reload() }

    val refresh = { scope.launch { reload() } }
    // Отбор и порядок считаются от прочитанного и от условий, а не на
    // каждый кадр: две тысячи точек и пять тысяч касс пересобирались бы
    // при каждом нажатии клавиши в любом поле экрана.
    val locked = cabinet.blockedRegisters
    val rows = remember(places, cabinet.registers, place, sieve, locked, session.language) {
        placeRows(places, cabinet.registers, place, sieve, locked.orEmpty(), session.language)
    }
    // Карточка сменяет колонку на узком окне, а не встаёт под ней:
    // выбор точки её и открывает, возврат к списку — кнопкой над ней.
    // Выбранное при возврате остаётся выбранным.
    var detailShown by remember { mutableStateOf(false) }
    var listShown by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    TwoPane(
        split = if (session.placesCollapsed) CabinetPanes.railAndCard else CabinetPanes.placesAndReadingCard,
        modifier = Modifier.fillMaxSize(),
        narrow = NarrowPanes.Switched(showSecond = detailShown && (place != null || register != null)),
        first = {
            // Колонка на экране или нет, знает только сама раскладка:
            // отсюда карточка и узнаёт, нужна ли ей кнопка возврата.
            DisposableEffect(Unit) {
                listShown = true
                onDispose { listShown = false }
            }
            Row(modifier = Modifier.fillMaxSize()) {
                PlaceTree(
                    texts = texts,
                    language = session.language,
                    collapsed = session.placesCollapsed,
                    onToggle = { session.togglePlaces() },
                    rows = rows,
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
                    sieve = sieve,
                    onSieve = { sieve = it },
                    // Блокировку кабинет отдаёт не списком касс, а сводкой:
                    // до её ответа плашка блокировки погашена, а не обманывает
                    // пустым списком.
                    locksKnown = locked != null,
                    place = place,
                    register = register,
                    onPlace = {
                        place = it
                        register = null
                        detailShown = true
                    },
                    onRegister = {
                        register = it
                        detailShown = true
                    },
                    footer = { PlaceCreateButtons(session, cabinet, texts, place) { refresh() } },
                    listState = listState,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        second = {
            val back = if (listShown) null else ({ detailShown = false })
            PlaceDetail(session, cabinet, texts, places, place, register, back) { refresh() }
        }
    )
}
