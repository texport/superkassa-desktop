package kz.mybrain.superkassa.desktop.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kotlin.math.roundToInt

/**
 * Ярлычок на карте: место и сколько касс в нём.
 *
 * Кассы рисовались булавками на полотне, и у полотна два недостатка,
 * которые здесь и решаются: на нём нельзя написать число — рисование
 * идёт вне композиции и шрифтов не знает, — и нажатие по нему приходится
 * ловить расчётом расстояния до каждой булавки. Ярлычок же — обычная
 * поверхность Material: у неё своя надпись, своя тень и своё нажатие.
 *
 * @param label число касс места; пусто — касса одна, и вместо числа значок.
 * @param tone цвет состояния места: по нему видно, куда надо подойти.
 */
data class MapMark(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val label: String?,
    val tone: Color,
    val chosen: Boolean
)

/**
 * Ярлычки поверх карты.
 *
 * Стоят в том же окне, что и плитки, и считают своё место той же
 * проекцией: карта сдвинулась — сдвинулись и они. Раскрытый рисуется
 * последним, поверх соседей: его сейчас читают.
 *
 * Ушедшие за край окна не рисуются вовсе. На карте страны это половина
 * сети: показ их всё равно обрезает, а composable-ярлычок стоит дороже
 * точки на полотне.
 */
@Composable
fun MapMarks(state: MapState, canvas: IntSize, marks: List<MapMark>, onPick: (MapMark) -> Unit) {
    if (canvas.width == 0 || canvas.height == 0) return
    val corner = MapProjection.corner(
        state.centerLatitude,
        state.centerLongitude,
        state.zoom,
        canvas.width,
        canvas.height
    )
    marks.sortedBy { it.chosen }.forEach { mark ->
        val at = MapProjection.screen(mark.latitude, mark.longitude, state.zoom, corner)
        if (inside(at, canvas)) Mark(mark, at, onPick)
    }
}

/** Попадает ли место в окно карты — с запасом на сам ярлычок. */
private fun inside(at: MapPixel, canvas: IntSize): Boolean =
    at.x > -EDGE && at.y > -EDGE && at.x < canvas.width + EDGE && at.y < canvas.height + EDGE

/**
 * Сам ярлычок.
 *
 * Залит поверхностью, а не цветом состояния: цветом стоят обводка
 * и надпись, и на светлой улице ярлычок читается так же, как на тёмном
 * лесу. Раскрытый меняется ролями — заливка главным цветом, — и виден
 * среди соседей сразу.
 */
@Composable
private fun Mark(mark: MapMark, at: MapPixel, onPick: (MapMark) -> Unit) {
    val filled = mark.chosen
    Surface(
        onClick = { onPick(mark) },
        shape = CircleShape,
        color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (filled) MaterialTheme.colorScheme.onPrimary else mark.tone,
        border = BorderStroke(Sizes.mapMarkEdge, mark.tone),
        shadowElevation = Sizes.mapMarkLift,
        modifier = Modifier
            .offset { IntOffset(at.x.roundToInt(), at.y.roundToInt()) }
            // Ярлычок стоит серединой на месте, а не углом: угол уводил
            // бы его вниз-вправо от дома на полтора десятка точек.
            .graphicsLayer {
                translationX = -size.width / 2f
                translationY = -size.height / 2f
            }
            .clip(CircleShape)
    ) {
        MarkBody(mark.label)
    }
}

/** Содержимое ярлычка: число касс или значок места у одиночной. */
@Composable
private fun MarkBody(label: String?) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = Sizes.mapMark, minHeight = Sizes.mapMark)
            .padding(horizontal = Spacing.tight),
        contentAlignment = Alignment.Center
    ) {
        if (label == null) {
            Icon(AppIcons.place, contentDescription = null, modifier = Modifier.size(Sizes.chipIcon))
        } else {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Запас за краем окна, в пределах которого ярлычок ещё рисуется. */
private const val EDGE = 48.0
