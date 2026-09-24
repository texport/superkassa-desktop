package kz.mybrain.superkassa.presentation.cabinet.component

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.picker.PickerWords
import kz.mybrain.superkassa.designsystem.picker.SearchablePicker
import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.presentation.cabinet.addressIn
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

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
 * Точки — [PlaceOptions]: найденные и чем искать дальше и заводить новую.
 */
@Composable
internal fun PlacePicker(
    label: String,
    texts: CabinetTexts,
    language: Language,
    options: PlaceOptions,
    selected: RetailPlace?,
    onSelect: (RetailPlace) -> Unit
) {
    SearchablePicker(
        label = label,
        options = options.places,
        selected = selected,
        words = PickerWords({ placeTitle(language, it) }, ::placeSearchKeys, texts.placeNotFound),
        onQuery = options.onQuery,
        onSelect = onSelect
    )
    options.onCreate?.let { create -> TextButton(onClick = create) { Text(texts.addPlace) } }
}

/**
 * Торговые точки для выбора.
 *
 * @property places точки, из которых выбирают: весь список или найденные кабинетом.
 * @property onQuery набранное — кабинету, если точки ищет он.
 * @property onCreate чем завести точку, которой ещё нет; `null` — нечем.
 *   Кнопка стоит под полем, а не строкой внутри списка: среди двух тысяч
 *   строк её пришлось бы искать.
 */
internal class PlaceOptions(
    val places: List<RetailPlace>,
    val onQuery: (String) -> Unit = {},
    val onCreate: (() -> Unit)? = null
)

/** Как названа точка в поле и в списке: своё название, а за ним адрес. */
private fun placeTitle(language: Language, place: RetailPlace): String =
    listOf(place.name, addressIn(language, place.address, place.addressKz))
        .filter { it.isNotBlank() }
        .joinToString(Glyphs.SEPARATOR)

/** По чему находится точка: по названию и по адресу на обоих языках. */
private fun placeSearchKeys(place: RetailPlace): List<String> =
    listOfNotNull(place.name, place.address, place.addressKz)
