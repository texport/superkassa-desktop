package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.components.RecordRow
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.components.ScrollableList
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
    footer: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.width(if (collapsed) Sizes.rail else Sizes.registerColumn).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        TreeToggle(collapsed, texts.hints.placesTree, onToggle)
        if (collapsed) {
            PlaceRail(rows, place, register, onPlace, onRegister, Modifier.weight(1f))
            return@Column
        }
        SearchField(
            value = sieve.needle,
            label = texts.placeSearch,
            onChange = { onSieve(sieve.copy(needle = it)) },
            modifier = Modifier.fillMaxWidth().padding(end = Spacing.screen),
            clearLabel = texts.sieve.clear
        )
        PlaceSieveBar(texts, sieve, locksKnown, onSieve)
        val state = when {
            loading -> ScreenState.Working
            rows.isNotEmpty() -> ScreenState.Ready
            // Прочитанное показывается и при отказе: он мог настигнуть
            // дочитывание сороковой страницы, и прятать за ним первые
            // тридцать девять незачем.
            trouble != null -> ScreenState.Trouble(trouble, onRetry = onRetry)
            else -> treeEmpty(texts, sieve)
        }
        // Счёт стоит над строками: без строк считать нечего, а над словами
        // отказа «Показано 0 из 1004» читается как потеря тысячи точек.
        if (state !is ScreenState.Trouble && !loading) PlaceCount(texts, rows, total)
        ScreenSlot(state, Modifier.weight(1f)) {
            TreeRows(texts, language, rows, place, register, onPlace, onRegister, Modifier.weight(1f))
        }
        footer()
    }
}

/** Строки дерева: точка, под раскрытой — её кассы с отступом. */
@Composable
private fun TreeRows(
    texts: CabinetTexts,
    language: Language,
    rows: List<PlaceRow>,
    place: String?,
    register: String?,
    onPlace: (String) -> Unit,
    onRegister: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    ScrollableList(modifier = modifier) {
        items(items = rows, key = { it.id }) { row ->
            when (row) {
                // Название точки — в две строки: у сети оно длинное
                // и различается концом — «…Достык Плаза» отдел 12», —
                // а в одну строку все отделы обрывались одинаково.
                is PlaceRow.Point -> RecordRow(
                    title = row.place.name,
                    support = { PointSupport(texts, language, row.place) },
                    selected = row.id == place && register == null,
                    titleLines = NAME_LINES,
                    onClick = { onPlace(row.id) }
                )

                is PlaceRow.Register -> RecordRow(
                    title = registerTitle(row.register),
                    subtitle = row.register.registrationNumber,
                    selected = row.id == register,
                    modifier = Modifier.padding(start = Spacing.normal),
                    onClick = { onRegister(row.id) },
                    trailing = { CabinetStatusChip(row.register.status, texts) }
                )
            }
        }
    }
}

/**
 * Кнопка сворачивания колонки.
 *
 * Значок и подписи те же, что у рельса разделов: два переключателя
 * в одном окне не должны выглядеть разными действиями.
 */
@Composable
private fun TreeToggle(collapsed: Boolean, hint: String, onToggle: () -> Unit) {
    val common = LocalStrings.current.common
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onToggle) {
            Icon(
                imageVector = AppIcons.menu,
                contentDescription = if (collapsed) common.expand else common.collapse
            )
        }
        // Свёрнутая колонка — рельс шириной в одну кнопку: второй значок
        // в неё не встаёт, да и объяснять нечего — списка не видно.
        if (!collapsed) InfoTip(hint)
    }
}

/** Сколько строк отводится названию точки в колонке. */
private const val NAME_LINES = 2
