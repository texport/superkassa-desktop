package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.desktop.ui.theme.Sizes

/**
 * Строки раскрытого списка выбора.
 *
 * Собираются по мере показа: в кабинете сети торговых точек и касс
 * по две тысячи, и столбец, собранный целиком, складывал их все на каждое
 * раскрытие — раскрытие списка стоило втрое дороже, чем такого же
 * короткого.
 *
 * Размер задаётся числом, а не содержимым, намеренно: раскрытый список
 * меряет содержимое по внутреннему размеру, а ленивый список такой меры
 * не даёт вовсе и падает на первом же раскрытии. Высота — по числу строк,
 * но не выше предела: дальше список прокручивается сам.
 *
 * @param width ширина поля, под которым раскрыт список.
 */
@Composable
fun <T> PickerRows(options: List<T>, width: Dp, row: @Composable (T) -> Unit) {
    LazyColumn(modifier = Modifier.size(width, rowsHeight(options.size))) {
        items(options) { option -> row(option) }
    }
}

/** Высота списка по числу строк, но не выше предела. */
internal fun rowsHeight(rows: Int): Dp = minOf(Sizes.pickerRow * rows, Sizes.pickerList)
