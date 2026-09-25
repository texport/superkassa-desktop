package kz.mybrain.superkassa.presentation.cabinet.places

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.PaneScaffoldScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.adaptive.listDetailDirective
import kz.mybrain.superkassa.designsystem.adaptive.listDetailValue
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.theme.size.CabinetPanes
import kz.mybrain.superkassa.navigation.step.PlaceCardKey
import kz.mybrain.superkassa.presentation.cabinet.CabinetUiState
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.places.component.PlaceCreateButtons
import kz.mybrain.superkassa.presentation.cabinet.places.component.PlaceRow
import kz.mybrain.superkassa.presentation.cabinet.places.component.PlaceTree
import kz.mybrain.superkassa.presentation.cabinet.places.component.placeRows
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.navigation.detailStep
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
 * Колонка и карточка — «список и подробности» Material 3: рядом, начиная
 * с расширенного окна, и поровну, а свёрнутая колонка — шириной рельса.
 * На узком окне выбранное открывается поверх колонки шагом истории окна
 * ([detailStep]): назад к колонке ведёт стрелка в шапке окна, жест
 * и Escape, а не своя кнопка над карточкой.
 *
 * @param stepped карточка открыта поверх колонки шагом истории окна.
 *
 * Поиск сужает обе части списка сразу: у сети бывают сотни точек, и найти
 * среди них кассу глазами нельзя. Отбор и порядок — там же: пять тысяч
 * касс, из которых на учёте единицы, поиском по названию не перебрать.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun PlacesScreen(cabinet: CabinetWindow, texts: CabinetTexts, stepped: Boolean = false) {
    val collapsed = cabinet.look.placesCollapsed()
    val model = placesViewModel(cabinet.cabinet)
    val chosen by model.state.collectAsScreenState()
    val listState = rememberLazyListState()
    val directive = listDetailDirective()
    val picked = chosen.place != null || chosen.register != null
    val step = detailStep(stepped, picked, beside = directive.maxHorizontalPartitions > 1)
    ListDetailPaneScaffold(
        directive = directive,
        value = listDetailValue(directive, step.over),
        listPane = {
            AnimatedPane(modifier = placesWidth(collapsed)) {
                PlacesColumn(cabinet, texts, model, chosen, listState, Modifier.fillMaxSize()) {
                    step.opened(PlaceCardKey)
                }
            }
        },
        detailPane = { AnimatedPane { PlaceDetail(cabinet, texts, chosen) } },
        modifier = Modifier.fillMaxSize()
    )
}

/** Ширина колонки: развёрнутая — долей окна, свёрнутая — рельсом. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private fun PaneScaffoldScope.placesWidth(collapsed: Boolean): Modifier = if (collapsed) {
    Modifier.preferredWidth(CabinetPanes.placesRail)
} else {
    Modifier.preferredWidth(CabinetPanes.PLACES_SHARE)
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

/** Строки колонки: пересобираются, только когда меняется то, из чего они собраны. */
@Composable
private fun rememberRows(window: CabinetUiState, chosen: PlacesUiState): List<PlaceRow> {
    val language = LocalLanguage.current
    val locked = window.blocked.orEmpty()
    return remember(window.places, window.registers, chosen.place, chosen.sieve, locked, language) {
        placeRows(window.places, window.registers, chosen.place, chosen.sieve, locked, language)
    }
}
