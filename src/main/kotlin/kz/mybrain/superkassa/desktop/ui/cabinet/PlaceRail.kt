package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import kz.mybrain.superkassa.desktop.ui.components.ScrollableList
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons

/**
 * Свёрнутая колонка торговых точек.
 *
 * Свёрнутая колонка остаётся рабочей: значок точки, под раскрытой точкой —
 * значки её касс, выбранное выделено, а название показывается подсказкой
 * при наведении. Прежде свёрнутая колонка не показывала ничего, кроме
 * кнопки разворачивания, и выбрать другую кассу можно было только
 * развернув список обратно.
 *
 * Собрана как рельс разделов — теми же `NavigationRailItem`, — но внутри
 * прокручиваемого списка, а не готового `NavigationRail`: тот держит
 * содержимое обычным столбцом, и пятьсот точек он собирал бы целиком
 * при каждой перерисовке.
 */
@Composable
internal fun PlaceRail(
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
                is PlaceRow.Point -> RailPoint(
                    icon = AppIcons.place,
                    name = row.place.name,
                    selected = row.id == place && register == null
                ) { onPlace(row.id) }

                is PlaceRow.Register -> RailPoint(
                    icon = AppIcons.kkm,
                    name = registerTitle(row.register),
                    selected = row.id == register
                ) { onRegister(row.id) }
            }
        }
    }
}

/**
 * Значок в свёрнутой колонке.
 *
 * Название уходит в подсказку по наведению: в колонку шириной рельса оно
 * не помещается, а без него значки точек неотличимы друг от друга.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RailPoint(icon: ImageVector, name: String, selected: Boolean, onClick: () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(name) } },
        state = rememberTooltipState()
    ) {
        NavigationRailItem(
            selected = selected,
            onClick = onClick,
            icon = { Icon(icon, contentDescription = name) }
        )
    }
}
