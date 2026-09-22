package kz.mybrain.superkassa.desktop.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Цвет по оттенку, насыщенности и тону — как в тональных палитрах Material 3.
 *
 * Тон Material — это светлота CIE L*: тон 40 читается на белом, тон 80 —
 * на чёрном, и это верно для любого оттенка. Поэтому цвета считаются
 * в пространстве Lab: там светлота задаётся напрямую, а оттенок и
 * насыщенность — углом и радиусом. Считать в RGB нельзя: одинаковые
 * числа там дают разную яркость, и жёлтая касса слепила бы, а синяя
 * тонула бы в тёмной теме.
 *
 * Не всякая насыщенность помещается в экранные цвета: у пастельных
 * тонов запас невелик. Насыщенность понижается до первой, что
 * помещается, — так же поступает и Material.
 */
internal object Tones {

    fun of(hue: Float, chroma: Float, tone: Int): Color {
        var fitting = chroma
        while (fitting > 0f && !fits(hue, fitting, tone)) fitting -= CHROMA_STEP
        val (r, g, b) = linear(hue, fitting.coerceAtLeast(0f), tone)
        return Color(gamma(r), gamma(g), gamma(b))
    }

    private fun fits(hue: Float, chroma: Float, tone: Int): Boolean {
        val (r, g, b) = linear(hue, chroma, tone)
        return listOf(r, g, b).all { it >= -TOLERANCE && it <= 1f + TOLERANCE }
    }

    /** Линейные компоненты sRGB, ещё без гамма-кривой и без обрезки. */
    private fun linear(hue: Float, chroma: Float, tone: Int): Triple<Float, Float, Float> {
        val radians = Math.toRadians(hue.toDouble())
        val a = chroma * cos(radians)
        val b = chroma * sin(radians)
        val fy = (tone + LAB_OFFSET) / LAB_SCALE
        val fx = fy + a / LAB_A_SCALE
        val fz = fy - b / LAB_B_SCALE
        val x = WHITE_X * inverse(fx)
        val y = inverse(fy)
        val z = WHITE_Z * inverse(fz)
        return Triple(
            (RX * x + RY * y + RZ * z).toFloat(),
            (GX * x + GY * y + GZ * z).toFloat(),
            (BX * x + BY * y + BZ * z).toFloat()
        )
    }

    private fun inverse(t: Double): Double {
        val cubed = t * t * t
        return if (cubed > LAB_EPSILON) cubed else (t - LAB_OFFSET / LAB_SCALE) / LAB_KAPPA
    }

    private fun gamma(linear: Float): Float {
        val clipped = linear.coerceIn(0f, 1f)
        return if (clipped <= GAMMA_KNEE) {
            clipped * GAMMA_LINEAR
        } else {
            GAMMA_LIFT * clipped.pow(1f / GAMMA_POWER) - GAMMA_SHIFT
        }
    }

    private const val CHROMA_STEP = 0.5f
    private const val TOLERANCE = 0.0005f

    private const val LAB_OFFSET = 16.0
    private const val LAB_SCALE = 116.0
    private const val LAB_A_SCALE = 500.0
    private const val LAB_B_SCALE = 200.0
    private const val LAB_EPSILON = 0.008856
    private const val LAB_KAPPA = 7.787

    /** Белая точка D65: та, в которой калиброван экран. */
    private const val WHITE_X = 0.95047
    private const val WHITE_Z = 1.08883

    private const val RX = 3.2404542
    private const val RY = -1.5371385
    private const val RZ = -0.4985314
    private const val GX = -0.9692660
    private const val GY = 1.8760108
    private const val GZ = 0.0415560
    private const val BX = 0.0556434
    private const val BY = -0.2040259
    private const val BZ = 1.0572252

    private const val GAMMA_KNEE = 0.0031308f
    private const val GAMMA_LINEAR = 12.92f
    private const val GAMMA_LIFT = 1.055f
    private const val GAMMA_POWER = 2.4f
    private const val GAMMA_SHIFT = 0.055f
}
