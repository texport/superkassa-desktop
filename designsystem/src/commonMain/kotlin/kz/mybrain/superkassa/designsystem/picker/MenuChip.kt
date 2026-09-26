package kz.mybrain.superkassa.designsystem.picker

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

/**
 * Отбор, у которого значений десятки, — плашкой с выпадающим списком.
 *
 * Плашка стоит в одном ряду с остальными плашками отбора и одного с ними
 * роста и шрифта: рост, поля и надпись берутся у `FilterChip` Material 3,
 * а не задаются на месте. Прежде отбор по смене был полем ввода в 56 dp
 * и торчал из ряда плашек и сегментов, будто пришёл с другого экрана.
 *
 * Поле ввода здесь не годится по смыслу: значение выбирается из готового
 * набора, а не набирается, и `OutlinedTextField` только для чтения обещал
 * бы ввод, которого нет. Набор плашек тоже не годится: смен за месяц
 * шестьдесят, и они заняли бы пол-экрана.
 *
 * @param value как названо выбранное сейчас: оно и стоит на плашке.
 * @param chosen выбрано ли что-то, кроме «все»: выбранная плашка залита.
 * @param search поиск над списком, когда значений тысячи: см. [MenuSearch].
 */
@Composable
fun <T> MenuChip(
    value: String,
    options: List<T>,
    title: (T) -> String,
    chosen: Boolean,
    modifier: Modifier = Modifier,
    search: MenuSearch? = null,
    onSelect: (T) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    // Ширину, заданную рядом (доля строки отбора), плашка берёт целиком,
    // а без неё — по своей подписи.
    Box(modifier = modifier, propagateMinConstraints = true) {
        FilterChip(
            selected = chosen,
            onClick = { open = true },
            label = { Text(text = value, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingIcon = { ChipArrow() }
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MenuItems(options, title, search) { option ->
                onSelect(option)
                open = false
            }
        }
    }
}

/**
 * Пункты открытого меню — все или, при поиске, первые подходящие.
 *
 * Меню Material 3 собирает пункты разом, без ленивого списка: у сети
 * в две тысячи точек открытие меню собирало две тысячи строк, и окно
 * подтормаживало на каждом нажатии плашки. С поиском меню показывает
 * не больше [MenuSearch.limit] подходящих и говорит, что остальные
 * найдутся уточнением.
 */
@Composable
private fun <T> MenuItems(options: List<T>, title: (T) -> String, search: MenuSearch?, onPick: (T) -> Unit) {
    var query by remember { mutableStateOf("") }
    val found = remember(options, query) {
        if (query.isBlank()) options else options.filter { title(it).contains(query.trim(), ignoreCase = true) }
    }
    val limit = search?.limit ?: found.size
    search?.let { MenuSearchField(it, query) { entered -> query = entered } }
    found.take(limit).forEach { option ->
        DropdownMenuItem(
            text = { Text(text = title(option), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            onClick = { onPick(option) }
        )
    }
    if (search != null && found.size > limit) MenuMore(search.more)
}

/** Поле поиска над пунктами меню. */
@Composable
private fun MenuSearchField(search: MenuSearch, query: String, onQuery: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        label = { Text(search.label) },
        singleLine = true,
        modifier = Modifier.padding(horizontal = Spacing.fieldGap).fillMaxWidth()
    )
}

/** Строка под пунктами: подходящих больше, чем показано. */
@Composable
private fun MenuMore(words: String) {
    Text(
        text = words,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.fieldGap, vertical = Spacing.itemGap)
    )
}

/**
 * Поиск в меню плашки — для отбора, у которого значений тысячи.
 *
 * @param label подпись поля поиска.
 * @param more что сказать, когда подходящих больше, чем показано.
 * @param limit сколько подходящих показывать; остальное — уточнением.
 */
class MenuSearch(val label: String, val more: String, val limit: Int)

/** Стрелка выпадающего списка на плашке: по ней видно, что плашка не переключатель. */
@Composable
private fun ChipArrow() {
    Icon(
        imageVector = AppIcons.expand,
        contentDescription = null,
        modifier = Modifier.size(Sizes.chipIcon)
    )
}
