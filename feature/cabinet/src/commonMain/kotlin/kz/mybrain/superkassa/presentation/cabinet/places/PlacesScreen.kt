package kz.mybrain.superkassa.presentation.cabinet.places

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.adaptive.NarrowPanes
import kz.mybrain.superkassa.designsystem.adaptive.TwoPane
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.theme.size.CabinetPanes
import kz.mybrain.superkassa.presentation.cabinet.CabinetUiState
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.places.component.PlaceCreateButtons
import kz.mybrain.superkassa.presentation.cabinet.places.component.PlaceRow
import kz.mybrain.superkassa.presentation.cabinet.places.component.PlaceTree
import kz.mybrain.superkassa.presentation.cabinet.places.component.placeRows
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

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
 * Колонка и карточка делят место долями [CabinetPanes.placesAndCard],
 * а не постоянной шириной колонки: на планшете стоймя колонка в четыреста
 * точек оставляла карточке столбик в букву шириной. Где обеим тесно,
 * карточка сменяет колонку целиком.
 *
 * Поиск сужает обе части списка сразу: у сети бывают сотни точек, и найти
 * среди них кассу глазами нельзя. Отбор и порядок — там же: пять тысяч
 * касс, из которых на учёте единицы, поиском по названию не перебрать.
 */
@Composable
internal fun PlacesScreen(cabinet: CabinetWindow, texts: CabinetTexts) {
    val collapsed = cabinet.look.placesCollapsed()
    val model = placesViewModel(cabinet.cabinet)
    val chosen by model.state.collectAsScreenState()
    var detailShown by remember { mutableStateOf(false) }
    var listShown by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()
    TwoPane(
        split = if (collapsed) CabinetPanes.railAndCard else CabinetPanes.placesAndCard,
        modifier = Modifier.fillMaxSize(),
        narrow = NarrowPanes.Switched(showSecond = detailShown && (chosen.place != null || chosen.register != null)),
        first = {
            // Колонка на экране или нет, знает только сама раскладка:
            // отсюда карточка и узнаёт, нужна ли ей кнопка возврата.
            OnScreen { listShown = it }
            Row(modifier = Modifier.fillMaxSize()) {
                PlacesColumn(cabinet, texts, model, chosen, listState, Modifier.weight(1f)) { detailShown = true }
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
        second = {
            val back = if (listShown) null else ({ detailShown = false })
            PlaceDetail(cabinet, texts, chosen, back)
        }
    )
}

/**
 * Колонка точек с кассами под ними; выбор строки открывает карточку.
 *
 * @param onOpened выбрана точка или касса: на узком окне карточка сменяет колонку.
 */
@Composable
private fun PlacesColumn(
    cabinet: CabinetWindow,
    texts: CabinetTexts,
    model: PlacesViewModel,
    chosen: PlacesUiState,
    listState: LazyListState,
    modifier: Modifier,
    onOpened: () -> Unit
) {
    val window by cabinet.cabinet.state.collectAsScreenState()
    PlaceTree(
        texts = texts,
        language = LocalLanguage.current,
        collapsed = cabinet.look.placesCollapsed(),
        onToggle = cabinet.look.togglePlaces,
        rows = rememberRows(window, chosen),
        // Сколько точек у компании — по словам кабинета: пока список
        // дочитывается, прочитано меньше, и колонка об этом говорит.
        total = window.shownTotal,
        // Строки встают, как только пришла первая страница: у сети их
        // сорок, и ожидание до последней заняло бы весь показ.
        loading = !window.placesRead && window.places.isEmpty(),
        // Почему список пуст: кабинет отказал или у владельца и правда
        // нет ни одной точки. Слова — те же, какими отказал кабинет.
        trouble = window.placesTrouble.takeIf { window.places.isEmpty() },
        onRetry = cabinet.cabinet::reload,
        sieve = chosen.sieve,
        onSieve = model::sieve,
        // Блокировку кабинет отдаёт не списком касс, а сводкой:
        // до её ответа плашка блокировки погашена, а не обманывает
        // пустым списком.
        locksKnown = window.blocked != null,
        place = chosen.place,
        register = chosen.register,
        onPlace = { model.selectPlace(it).also { onOpened() } },
        onRegister = { model.selectRegister(it).also { onOpened() } },
        footer = { PlaceCreateButtons(cabinet, texts, chosen.place) },
        listState = listState,
        modifier = modifier
    )
}

/** Сообщает, стоит ли содержимое на экране: раскладка убирает его и возвращает сама. */
@Composable
private fun OnScreen(shown: (Boolean) -> Unit) {
    DisposableEffect(Unit) {
        shown(true)
        onDispose { shown(false) }
    }
}

/** Строки колонки: пересобираются, только когда меняется то, из чего они собраны. */
@Composable
private fun rememberRows(window: CabinetUiState, chosen: PlacesUiState): List<PlaceRow> {
    val language = LocalLanguage.current
    val locked = window.blocked.orEmpty()
    return remember(window.places, window.registers, chosen.place, chosen.sieve, locked, language) {
        placeRows(window.places, window.registers, chosen.place, chosen.sieve, locked, language)
    }
}
