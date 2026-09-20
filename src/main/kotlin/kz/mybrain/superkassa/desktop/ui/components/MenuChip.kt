package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes

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
 */
@Composable
fun <T> MenuChip(
    value: String,
    options: List<T>,
    title: (T) -> String,
    chosen: Boolean,
    onSelect: (T) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = chosen,
            onClick = { open = true },
            label = { Text(text = value, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            trailingIcon = { ChipArrow() }
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = title(option), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        onSelect(option)
                        open = false
                    }
                )
            }
        }
    }
}

/** Стрелка выпадающего списка на плашке: по ней видно, что плашка не переключатель. */
@Composable
private fun ChipArrow() {
    Icon(
        imageVector = AppIcons.expand,
        contentDescription = null,
        modifier = Modifier.size(Sizes.chipIcon)
    )
}
