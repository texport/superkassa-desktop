package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.ui.components.SearchablePicker
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs

/**
 * Выбор торговой точки — набором, а не перебором.
 *
 * Точку выбирают дважды: заводя кассу и подавая заявление о перерегистрации.
 * Оба раза здесь стоял простой выпадающий список, раскрытый целиком, —
 * и это было верно, пока точек было пять. У сети их тысячи: владелец
 * крутил колесом список из двух тысяч строк, а найти в нём «Магазин
 * на Абая» глазами нельзя.
 *
 * Ищется точка и по названию, и по адресу: название владелец придумал сам
 * и мог забыть, а адрес — то, чем точка и опознаётся. По той же причине
 * адрес стоит рядом с названием и в самом списке: три магазина одной сети
 * различаются только им.
 *
 * @param onCreate чем завести точку, которой ещё нет; `null` — нечем.
 *   Кнопка стоит под полем, а не строкой внутри списка: среди двух тысяч
 *   строк её пришлось бы искать.
 */
@Composable
internal fun PlacePicker(
    label: String,
    texts: CabinetTexts,
    language: Language,
    places: List<RetailPlace>,
    selected: RetailPlace?,
    onSelect: (RetailPlace) -> Unit,
    onCreate: (() -> Unit)? = null
) {
    SearchablePicker(
        label = label,
        options = places,
        selected = selected,
        title = { placeTitle(language, it) },
        keys = { placeSearchKeys(it) },
        notFound = texts.placeNotFound,
        onSelect = onSelect
    )
    if (onCreate != null) {
        TextButton(onClick = onCreate) { Text(texts.addPlace) }
    }
}

/** Как названа точка в поле и в списке: своё название, а за ним адрес. */
private fun placeTitle(language: Language, place: RetailPlace): String =
    listOf(place.name, addressIn(language, place.address, place.addressKz))
        .filter { it.isNotBlank() }
        .joinToString(Glyphs.SEPARATOR)

/** По чему находится точка: по названию и по адресу на обоих языках. */
private fun placeSearchKeys(place: RetailPlace): List<String> =
    listOfNotNull(place.name, place.address, place.addressKz)
