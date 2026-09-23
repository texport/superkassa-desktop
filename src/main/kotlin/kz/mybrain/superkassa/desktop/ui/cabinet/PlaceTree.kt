package kz.mybrain.superkassa.desktop.ui.cabinet

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.components.SearchField
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

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
 * Свёрнутая колонка не пустеет — она становится рельсом значков.
 *
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
    collapsed: Boolean,
    onToggle: () -> Unit,
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
    modifier: Modifier = Modifier.width(if (collapsed) Sizes.rail else Sizes.registerColumn)
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
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
        TreeHead(texts, collapsed, onToggle) {
            // Счёт стоит над строками: без строк считать нечего, а над словами
            // отказа «Показано 0 из 1004» читается как потеря тысячи точек.
            if (state !is ScreenState.Trouble && !loading) PlaceCount(texts, rows, total, Modifier.weight(1f))
        }
        if (collapsed) {
            PlaceRail(rows, place, register, onPlace, onRegister, Modifier.weight(1f))
            return@Column
        }
        TreeRows(texts, language, rows, state, place, register, onPlace, onRegister, listState, Modifier.weight(1f)) {
            SearchField(
                value = sieve.needle,
                label = texts.placeSearch,
                onChange = { onSieve(sieve.copy(needle = it)) },
                modifier = Modifier.fillMaxWidth(),
                clearLabel = texts.sieve.clear
            )
            PlaceSieveBar(texts, sieve, locksKnown, onSieve)
        }
        footer()
    }
}

/**
 * Шапка колонки: сворачивание, счёт точек и подсказка одной строкой.
 *
 * Прежде кнопка сворачивания с подсказкой занимали свою строку, а счёт —
 * ещё одну над списком: в окне 960×640 это была высота строки списка,
 * и из двух тысяч точек было видно полторы.
 *
 * Значок и подписи сворачивания те же, что у рельса разделов: два
 * переключателя в одном окне не должны выглядеть разными действиями.
 * Свёрнутая колонка — рельс шириной в одну кнопку: счёт и подсказка
 * в неё не встают, да и объяснять нечего — списка не видно.
 */
@Composable
private fun TreeHead(
    texts: CabinetTexts,
    collapsed: Boolean,
    onToggle: () -> Unit,
    count: @Composable RowScope.() -> Unit
) {
    val common = LocalStrings.current.common
    Row(
        modifier = if (collapsed) Modifier else Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = AppIcons.menu,
                contentDescription = if (collapsed) common.expand else common.collapse
            )
        }
        if (collapsed) return@Row
        InfoTip(texts.hints.placesTree)
        count()
    }
}
