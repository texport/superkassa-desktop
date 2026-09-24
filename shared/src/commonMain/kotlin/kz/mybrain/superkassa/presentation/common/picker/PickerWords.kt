package kz.mybrain.superkassa.presentation.common.picker

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Как значение набора называется и ищется.
 *
 * @property title как назвать значение в поле и в списке.
 * @property keys по чему искать: название и код — владелец набирает то, что
 *   помнит, а помнит он либо одно, либо другое.
 * @property notFound строка о том, что по набранному ничего нет.
 */
class PickerWords<T>(val title: (T) -> String, val keys: (T) -> List<String>, val notFound: String)

/** Что набрано в поле и раскрыт ли список. */
internal class PickerTyping(initial: String) {
    var query by mutableStateOf(initial)
    var typed by mutableStateOf(false)
    var open by mutableStateOf(false)

    fun type(text: String) {
        query = text
        typed = true
        open = true
    }

    fun pick(title: String) {
        open = false
        typed = false
        query = title
    }
}
