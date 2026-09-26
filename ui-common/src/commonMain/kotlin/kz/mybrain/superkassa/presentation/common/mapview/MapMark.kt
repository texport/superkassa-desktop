package kz.mybrain.superkassa.presentation.common.mapview

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
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kotlin.math.roundToInt

/**
 * Ярлычок на карте: место и сколько точек в нём.
 *
 * Кассы рисовались булавками на полотне, и у полотна два недостатка,
 * которые здесь и решаются: на нём нельзя написать число — рисование
 * идёт вне композиции и шрифтов не знает, — и нажатие по нему приходится
 * ловить расчётом расстояния до каждой булавки. Ярлычок же — обычная
 * поверхность Material: у неё своя надпись, своя тень и своё нажатие.
 *
 * @param count сколько точек сошлось в этом месте; одна — вместо числа значок.
 * @param tone цвет состояния места: по нему видно, куда надо подойти.
 */
data class MapMark(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val count: Int,
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
 * @param cell сторона клетки, по которой места сведены, в точках полотна;
 *   0 — ярлычки стоят точно в своих градусах.
 *
 * Ярлычки мест, сведённых по клеткам, не выходят за свою клетку ([cell]):
 * иначе ярлычок у края клетки ложился на соседний.
 *
 * Ушедшие за край окна не рисуются вовсе. На карте страны это половина
 * сети: показ их всё равно обрезает, а composable-ярлычок стоит дороже
 * точки на полотне.
 */
@Composable
fun MapMarks(
    state: MapState,
    canvas: IntSize,
    marks: List<MapMark>,
    cell: Double = 0.0,
    onPick: (MapMark) -> Unit
) {
    if (canvas.width == 0 || canvas.height == 0) return
    val corner = MapProjection.corner(
        state.centerLatitude,
        state.centerLongitude,
        state.zoom,
        canvas.width,
        canvas.height
    )
    val density = LocalDensity.current
    marks.sortedBy { it.chosen }.forEach { mark ->
        if (!inside(MapProjection.screen(mark.latitude, mark.longitude, state.zoom, corner), canvas)) return@forEach
        key(mark.id) {
            val radius = with(density) { markSide(mark.count).toPx() } / 2.0
            // Место считается при раскладке, а не при сборке: сдвиг карты
            // двигает ярлычок, не пересобирая его. Пересобирались все
            // ярлычки окна на каждом кадре перетаскивания — сотни поверхностей
            // с тенью, и на планшете карта касс шла рывками.
            val place = remember(mark, state, canvas, cell, radius) { { placeOf(mark, state, canvas, cell, radius) } }
            Mark(mark, place, onPick)
        }
    }
}

/** Где стоит ярлычок сейчас: читается при раскладке, по свежему центру карты. */
private fun placeOf(mark: MapMark, state: MapState, canvas: IntSize, cell: Double, radius: Double): MapPixel {
    val corner = MapProjection.corner(
        state.centerLatitude,
        state.centerLongitude,
        state.zoom,
        canvas.width,
        canvas.height
    )
    val at = MapProjection.screen(mark.latitude, mark.longitude, state.zoom, corner)
    return if (cell > 0) MapProjection.keptInCell(at, corner, cell, radius) else at
}

/** Попадает ли место в окно карты — с запасом на сам ярлычок. */
fun inside(at: MapPixel, canvas: IntSize): Boolean =
    at.x > -MARK_EDGE && at.y > -MARK_EDGE && at.x < canvas.width + MARK_EDGE && at.y < canvas.height + MARK_EDGE

/**
 * Сам ярлычок.
 *
 * Залит поверхностью, а не цветом состояния: цветом стоят обводка
 * и надпись, и на светлой улице ярлычок читается так же, как на тёмном
 * лесу. Выбранный меняется ролями — заливка главным цветом, толще
 * обводка и выше тень, — и виден среди сотни соседей сразу.
 */
@Composable
private fun Mark(mark: MapMark, place: () -> MapPixel, onPick: (MapMark) -> Unit) {
    val filled = mark.chosen
    Surface(
        onClick = { onPick(mark) },
        shape = CircleShape,
        color = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = if (filled) MaterialTheme.colorScheme.onPrimary else mark.tone,
        border = BorderStroke(if (filled) Sizes.mapMarkEdgeChosen else Sizes.mapMarkEdge, mark.tone),
        shadowElevation = if (filled) Sizes.mapMarkLiftChosen else Sizes.mapMarkLift,
        modifier = Modifier
            .offset { place().let { IntOffset(it.x.roundToInt(), it.y.roundToInt()) } }
            // Ярлычок стоит серединой на месте, а не углом: угол уводил
            // бы его вниз-вправо от дома на полтора десятка точек.
            .graphicsLayer {
                translationX = -size.width / 2f
                translationY = -size.height / 2f
            }
            .clip(CircleShape)
    ) {
        MarkBody(mark.count)
    }
}

/** Содержимое ярлычка: число точек или значок места у одиночного. */
@Composable
private fun MarkBody(count: Int) {
    val side = markSide(count)
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = side, minHeight = side)
            .padding(horizontal = Spacing.itemGap),
        contentAlignment = Alignment.Center
    ) {
        if (count <= 1) {
            Icon(AppIcons.place, contentDescription = null, modifier = Modifier.size(Sizes.chipIcon))
        } else {
            // Полужирное начертание, а не обычное: число стоит на кружке
            // поверх пёстрых плиток, и тонкие цифры на нём размывались.
            Text(text = count.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * Поперечник ярлычка по числу точек в нём.
 *
 * Ступенями: кружок места, кружок города и кружок области видны один
 * рядом с другим, а рост от самого числа дал бы полсотни почти
 * одинаковых размеров и ничего бы не сказал.
 */
private fun markSide(count: Int): Dp = when {
    count >= CROWD -> Sizes.mapMarkCrowd
    count >= MANY -> Sizes.mapMarkMany
    count > 1 -> Sizes.mapMarkFew
    else -> Sizes.mapMark
}

/** С этого числа точек место считается городом, а не домом. */
private const val MANY = 10

/** С этого — областью: столько точек сходится в одну лишь на карте страны. */
private const val CROWD = 50

/**
 * Запас за краем окна, в пределах которого ярлычок ещё рисуется.
 *
 * Открыт наружу вместе с [inside]: мест на карте страны тысячи, а в окно
 * попадает десяток, и складывать ярлычок для каждого — работа впустую.
 * Отсеивает лишние тот, кто местами владеет, и запас у него тот же.
 */
internal const val MARK_EDGE: Double = 48.0
