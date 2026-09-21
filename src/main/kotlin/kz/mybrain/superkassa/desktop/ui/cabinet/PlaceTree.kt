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
 * @param rows готовые строки дерева: сузил ли их поиск, знает [placeRows].
 * @param loading ответа кабинета ещё не было: на месте строк ожидание.
 * @param footer кнопки создания под списком.
 */
@Composable
internal fun PlaceTree(
    texts: CabinetTexts,
    collapsed: Boolean,
    onToggle: () -> Unit,
    rows: List<PlaceRow>,
    loading: Boolean,
    query: String,
    onQuery: (String) -> Unit,
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
        TreeToggle(collapsed, texts.placesTreeHint, onToggle)
        if (collapsed) {
            PlaceRail(rows, place, register, onPlace, onRegister, Modifier.weight(1f))
            return@Column
        }
        SearchField(
            value = query,
            label = texts.placeSearch,
            onChange = onQuery,
            modifier = Modifier.fillMaxWidth().padding(end = Spacing.screen)
        )
        val state = when {
            loading -> ScreenState.Working
            rows.isEmpty() -> treeEmpty(texts, query)
            else -> ScreenState.Ready
        }
        ScreenSlot(state, Modifier.weight(1f)) {
            TreeRows(texts, rows, place, register, onPlace, onRegister, Modifier.weight(1f))
        }
        footer()
    }
}

/** Строки дерева: точка, под раскрытой — её кассы с отступом. */
@Composable
private fun TreeRows(
    texts: CabinetTexts,
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
                is PlaceRow.Point -> RecordRow(
                    title = row.place.name,
                    subtitle = "${texts.registerCount}: ${row.place.cashRegisterCount}",
                    selected = row.id == place && register == null,
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
 * Пусто по-разному: точек нет вовсе или их не нашёл поиск.
 *
 * Владельцу с пятьюстами точками «заведите первую точку» вместо ненайденного
 * говорит о его хозяйстве неправду.
 */
private fun treeEmpty(texts: CabinetTexts, query: String): ScreenState.Empty {
    val searching = query.isNotBlank()
    return ScreenState.Empty(
        icon = if (searching) AppIcons.find else AppIcons.newKkm,
        title = if (searching) texts.placeNotFound else texts.placesEmpty,
        hint = if (searching) texts.placeNotFoundHint else texts.placesEmptyHint
    )
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
