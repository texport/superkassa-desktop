package kz.mybrain.superkassa.presentation.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.components.RecordRow
import kz.mybrain.superkassa.presentation.components.ScreenSlot
import kz.mybrain.superkassa.presentation.components.ScreenState
import kz.mybrain.superkassa.presentation.components.ScrollableList
import kz.mybrain.superkassa.presentation.strings.CabinetTexts
import kz.mybrain.superkassa.presentation.strings.Language
import kz.mybrain.superkassa.presentation.theme.Spacing

/**
 * Строки дерева: точка, под раскрытой — её кассы с отступом.
 *
 * Поиск и отбор стоят первой строкой того же списка и уезжают вверх
 * вместе с точками. Закреплённые над списком, они в узкой колонке
 * переносились на три-четыре строки и в окне 960×640 оставляли точкам
 * одну. Счёт найденного и кнопки создания остаются на месте: первый —
 * в шапке колонки, вторые — под списком.
 *
 * Ожидание, пустой ответ и отказ встают на место строк, а не всего
 * списка: поиск и отбор над ними остаются, и сузивший список до пустоты
 * владелец тут же расширяет его обратно.
 */
@Composable
internal fun TreeRows(
    texts: CabinetTexts,
    language: Language,
    rows: List<PlaceRow>,
    state: ScreenState,
    place: String?,
    register: String?,
    onPlace: (String) -> Unit,
    onRegister: (String) -> Unit,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    sieve: @Composable ColumnScope.() -> Unit
) {
    ScrollableList(modifier = modifier, state = listState) {
        item(key = SIEVE_KEY) {
            Column(
                modifier = Modifier.padding(bottom = Spacing.tight),
                verticalArrangement = Arrangement.spacedBy(Spacing.tight),
                content = sieve
            )
        }
        if (state != ScreenState.Ready) {
            item(key = STATE_KEY) { ScreenSlot(state, Modifier.fillParentMaxWidth()) {} }
            return@ScrollableList
        }
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

/** Сколько строк отводится названию точки в колонке. */
private const val NAME_LINES = 2

/** Ключи строк списка, которые не точки и не кассы: у тех ключ — их id. */
private const val SIEVE_KEY = "place-tree-sieve"
private const val STATE_KEY = "place-tree-state"
