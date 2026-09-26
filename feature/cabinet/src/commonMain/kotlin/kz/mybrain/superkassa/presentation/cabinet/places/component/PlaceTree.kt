package kz.mybrain.superkassa.presentation.cabinet.places.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.field.SearchField
import kz.mybrain.superkassa.designsystem.section.BarAction
import kz.mybrain.superkassa.designsystem.state.ScreenState
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.tip.InfoTip
import kz.mybrain.superkassa.presentation.cabinet.places.PlaceSieve
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Колонка слева: точки и кассы раскрытой точки, под ними — создание того
 * и другого.
 *
 * Строки идут списком, который держит на экране только видимое: у сети
 * бывают сотни точек и столько же касс, и собранный целиком столбец
 * отрисовывал бы весь список ради двух десятков видимых строк.
 *
 * Карточки вокруг списка нет намеренно: рамка вокруг пятисот строк обещает
 * обозримый раздел, а колонку от карточки кассы и так отделяет черта.
 *
 * @param onCollapse убрать колонку, отдав окно карточке; `null` — убрать
 *   нельзя: на узком окне колонка и карточка и так сменяют друг друга.
 * @param language на каком языке показывать адрес точки: регистр отдаёт
 *   его и по-русски, и по-казахски.
 * @param rows готовые строки дерева: что из них убрали поиск и отбор
 *   и в каком они порядке, знает [placeRows].
 * @param total сколько точек у компании всего: по одному списку не видно,
 *   две их или две тысячи, а поиск без этого числа не отличает «нашлось
 *   три» от «их всего три».
 * @param loading ответа кабинета ещё не было: на месте строк ожидание.
 * @param trouble кабинет списка не отдал — его словами; `null` — отдал.
 * @param locksKnown ответил ли кабинет, какие кассы заблокированы.
 * @param footer кнопки создания под списком.
 * @param listState прокрутка списка точек. Хранится снаружи: на узком
 *   окне колонка уступает место карточке целиком, и по возврату список
 *   стоит там же, где владелец его оставил.
 * @param modifier место колонки. Сама по себе колонка встаёт прежней
 *   шириной; раздел кабинета отдаёт ей долю окна.
 */
@Composable
internal fun PlaceTree(
    texts: CabinetTexts,
    language: Language,
    onCollapse: (() -> Unit)?,
    rows: List<PlaceRow>,
    total: Int,
    loading: Boolean,
    trouble: String?,
    onRetry: () -> Unit,
    sieve: PlaceSieve,
    onSieve: (PlaceSieve) -> Unit,
    locksKnown: Boolean,
    place: String?,
    register: String?,
    onPlace: (String) -> Unit,
    onRegister: (String) -> Unit,
    footer: @Composable ColumnScope.() -> Unit,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier.width(Sizes.registerColumn)
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
    ) {
        val state = when {
            loading -> ScreenState.Working
            rows.isNotEmpty() -> ScreenState.Ready
            // Прочитанное показывается и при отказе: он мог настигнуть
            // дочитывание сороковой страницы, и прятать за ним первые
            // тридцать девять незачем.
            trouble != null -> ScreenState.Trouble(trouble, onRetry = onRetry)
            else -> treeEmpty(texts, sieve)
        }
        TreeHead(texts, onCollapse) {
            // Счёт стоит над строками: без строк считать нечего, а над словами
            // отказа «Показано 0 из 1004» читается как потеря тысячи точек.
            if (state !is ScreenState.Trouble && !loading) PlaceCount(texts, rows, total, Modifier.weight(1f))
        }
        TreeRows(texts, language, rows, state, place, register, onPlace, onRegister, listState, Modifier.weight(1f)) {
            SearchField(
                value = sieve.needle,
                label = texts.places.search,
                onChange = { onSieve(sieve.copy(needle = it)) },
                modifier = Modifier.fillMaxWidth(),
                clearLabel = texts.places.sieve.clear
            )
            PlaceSieveBar(texts, sieve, locksKnown, onSieve)
        }
        footer()
    }
}

/**
 * Шапка колонки: убрать колонку, подсказка и счёт точек одной строкой.
 *
 * Прежде кнопка сворачивания с подсказкой занимали свою строку, а счёт —
 * ещё одну над списком: в окне 960×640 это была высота строки списка,
 * и из двух тысяч точек было видно полторы.
 *
 * Свёрнутая колонка прежде оставалась рельсом одинаковых значков точек
 * и касс: при сотнях точек по нему ничего не найти, а кнопка разворота
 * стояла у края полосы, не по её середине. Теперь колонка убирается
 * целиком, и вернуть её можно кнопкой у края карточки ([ListToggle]).
 */
@Composable
private fun TreeHead(texts: CabinetTexts, onCollapse: (() -> Unit)?, count: @Composable RowScope.() -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        onCollapse?.let { ListToggle(shown = true, onToggle = it) }
        InfoTip(texts.hints.placesTree)
        count()
    }
}

/**
 * Показать или убрать колонку точек — значком «боковая панель», как
 * сворачивание списка у Material 3; название — в подсказке.
 *
 * @param shown колонка сейчас на экране: кнопка её убирает.
 */
@Composable
internal fun ListToggle(shown: Boolean, onToggle: () -> Unit) {
    val common = LocalStrings.current.general
    BarAction(
        icon = if (shown) AppIcons.menuOpen else AppIcons.menu,
        label = if (shown) common.collapse else common.expand,
        onClick = onToggle
    )
}
