package kz.mybrain.superkassa.desktop.ui.map

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sinh
import kotlin.math.tan

/**
 * Проекция карты: широта и долгота ↔ точка на полотне.
 *
 * Плиточные карты нарисованы в проекции Web Mercator: мир на увеличении
 * `z` — квадрат из `2^z` плиток по стороне, каждая по 256 точек. Отсюда
 * и все пересчёты: показ знает только точки, а заявление — только
 * градусы, и переводить между ними нужно в одном месте.
 *
 * Широта ограничена ±85,051129° — за этим пределом проекция уходит
 * в бесконечность, и карта там не нарисована вовсе.
 */
object MapProjection {

    /** Сторона плитки в точках — как её отдают все плиточные службы. */
    const val TILE: Int = 256

    /** Предел широты, за которым проекция не определена. */
    const val MAX_LATITUDE: Double = 85.05112878

    /** Сколько плиток по стороне мира на этом увеличении. */
    fun tiles(zoom: Int): Int = 1 shl zoom

    /** Ширина мира в точках. */
    fun world(zoom: Int): Double = tiles(zoom).toDouble() * TILE

    fun xOf(longitude: Double, zoom: Int): Double = (longitude + HALF_TURN) / FULL_TURN * world(zoom)

    fun yOf(latitude: Double, zoom: Int): Double {
        val bounded = latitude.coerceIn(-MAX_LATITUDE, MAX_LATITUDE)
        val radians = bounded * PI / HALF_TURN
        return (1 - ln(tan(radians) + 1 / cos(radians)) / PI) / 2 * world(zoom)
    }

    fun longitudeOf(x: Double, zoom: Int): Double = x / world(zoom) * FULL_TURN - HALF_TURN

    fun latitudeOf(y: Double, zoom: Int): Double {
        val n = PI - 2 * PI * y / world(zoom)
        return HALF_TURN / PI * atan(sinh(n))
    }

    /**
     * Клетка сетки, в которую попадает место.
     *
     * Нужна, чтобы сводить близкие кассы в один ярлычок: соседние
     * машины в одном доме на карте города — одна точка, и рисовать их
     * порознь значит рисовать кашу. Клетка меряется точками полотна,
     * поэтому с приближением она мельчает сама: на увеличении квартала
     * дома расходятся, и ярлычки расходятся с ними.
     *
     * @param side сторона клетки в точках полотна.
     */
    fun cell(latitude: Double, longitude: Double, zoom: Int, side: Double): MapCell =
        MapCell(floor(xOf(longitude, zoom) / side).toInt(), floor(yOf(latitude, zoom) / side).toInt())

    /** Номер плитки, в которую попадает точка полотна. */
    fun tileOf(pixel: Double): Int = floor(pixel / TILE).toInt()

    /**
     * Левый верхний угол окна карты в точках полотна мира.
     *
     * Вынесено из показа сюда: по этому углу считается место каждой
     * плитки и каждого знака. Когда знаков много, тот же расчёт нужен
     * и вне рисования — чтобы понять, по какой из касс пришлось нажатие.
     */
    fun corner(centerLatitude: Double, centerLongitude: Double, zoom: Int, width: Int, height: Int): MapPixel =
        MapPixel(xOf(centerLongitude, zoom) - width / 2.0, yOf(centerLatitude, zoom) - height / 2.0)

    /** Куда внутри окна карты попадают эти градусы. */
    fun screen(latitude: Double, longitude: Double, zoom: Int, corner: MapPixel): MapPixel =
        MapPixel(xOf(longitude, zoom) - corner.x, yOf(latitude, zoom) - corner.y)

    /**
     * Градусы в том виде, в каком их принимает кабинет.
     *
     * Шесть знаков после запятой — примерно десятая доля метра: точнее
     * торговую точку не ставят, а лишние знаки в заявлении выглядят
     * ложной точностью.
     */
    fun degrees(value: Double): BigDecimal =
        BigDecimal(value).setScale(DEGREE_SCALE, RoundingMode.HALF_UP).stripTrailingZeros()

    private const val HALF_TURN = 180.0
    private const val FULL_TURN = 360.0

    /**
     * Крайняя долгота: дальше начинается та же сторона мира.
     *
     * Совпадает с половиной оборота не случайно — за половину оборота
     * от нулевого меридиана и лежит стык полушарий, — но смысл у них
     * разный, и меряются ими разные вещи.
     */
    const val MAX_LONGITUDE: Double = HALF_TURN

    private const val DEGREE_SCALE = 6
}

/**
 * Точка полотна карты.
 *
 * Одним типом меряются и мир целиком, и окно карты: в первом случае
 * это точка от начала мира, во втором — от левого верхнего угла окна.
 * Свой тип, а не пара чисел: перепутанные местами широта с долготой
 * и x с y — самая частая ошибка в этих пересчётах.
 */
data class MapPixel(val x: Double, val y: Double)

/** Клетка сетки полотна: по ней близкие места сводятся в одно. */
data class MapCell(val x: Int, val y: Int)
