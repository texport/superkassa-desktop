package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Что стоит над строками колонки точек и что — вместо них.
 *
 * Отделено от самой разметки колонки: счёт показанного и разбор пустоты
 * решают, что владелец прочитает о своём хозяйстве, и меняются вместе
 * с отбором, а не с раскладкой столбца.
 */

/**
 * Сколько точек показано и сколько их всего.
 *
 * Пока ничего не сужено, стоит одно число: владельцу сети важно видеть,
 * что кабинет отдал все две тысячи точек, а не первую страницу. Как
 * только поиск или отбор сузили список, рядом встаёт и общее число —
 * иначе «три точки» читается как всё хозяйство владельца.
 */
@Composable
internal fun PlaceCount(texts: CabinetTexts, rows: List<PlaceRow>, total: Int) {
    val shown = rows.count { it is PlaceRow.Point }
    Text(
        text = if (shown == total) "${texts.places}: $total" else texts.shownOf.format(shown, total),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = Spacing.tight, end = Spacing.screen)
    )
}

/**
 * Пусто по-разному, и путать эти случаи нельзя.
 *
 * Отбор по состоянию кассы ничего не оставил — так и сказано, теми же
 * словами, что на карте аналитики: у владельца пять тысяч касс, и ни
 * одной в спрошенном состоянии. Ненайденное поиском — своё: там владелец
 * набирал строку, и менять ему надо её, а не плашки. Пустое хозяйство —
 * третье: тут и правда нет ни одной точки. Отказ кабинета сюда не доходит
 * вовсе, его называет [ScreenState.Trouble].
 *
 * Владельцу с двумя тысячами точек «заведите первую точку» вместо
 * ненайденного говорит о его хозяйстве неправду.
 */
internal fun treeEmpty(texts: CabinetTexts, sieve: PlaceSieve): ScreenState.Empty = when {
    sieve.marked -> ScreenState.Empty(AppIcons.find, texts.sieve.empty, texts.sieve.emptyHint)
    sieve.needle.isNotBlank() -> ScreenState.Empty(AppIcons.find, texts.placeNotFound, texts.hints.placeNotFound)
    else -> ScreenState.Empty(AppIcons.newKkm, texts.placesEmpty, texts.hints.placesEmpty)
}
