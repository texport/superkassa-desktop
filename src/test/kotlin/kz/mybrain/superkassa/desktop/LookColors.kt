package kz.mybrain.superkassa.desktop

import androidx.compose.ui.graphics.Color
import kotlin.math.hypot
import kotlin.math.pow

/**
 * Мерки цвета для проверок оформления.
 *
 * Схем стало двадцать восемь — четырнадцать тонов на две темы, — и
 * глазами их не пересмотреть. Проверки считают то же, что считал бы
 * глаз: различает ли он надпись на подложке и различает ли два кружка
 * между собой.
 */
internal object LookColors {

    /**
     * Отношение контраста по WCAG: во сколько раз светлее из двух ярче.
     *
     * Та самая мерка, на которую опирается Material 3, назначая роли
     * тона: надпись на заливке — 4.5, знак и обводка — 3.
     */
    fun contrast(one: Color, other: Color): Float {
        val first = luminance(one)
        val second = luminance(other)
        return (maxOf(first, second) + OFFSET) / (minOf(first, second) + OFFSET)
    }

    /**
     * Насколько далеко два цвета разошлись для глаза — ΔE76 в Lab.
     *
     * В RGB такой мерки нет: одинаковая разница чисел даёт то заметное,
     * то неразличимое расхождение. Порог различения — около двойки.
     */
    fun distance(one: Color, other: Color): Float {
        val (l1, a1, b1) = lab(one)
        val (l2, a2, b2) = lab(other)
        return hypot((l1 - l2).toDouble(), hypot((a1 - a2).toDouble(), (b1 - b2).toDouble())).toFloat()
    }

    private fun luminance(color: Color): Float =
        RED_WEIGHT * linear(color.red) + GREEN_WEIGHT * linear(color.green) + BLUE_WEIGHT * linear(color.blue)

    private fun linear(channel: Float): Float =
        if (channel <= GAMMA_KNEE) channel / GAMMA_LINEAR else ((channel + GAMMA_SHIFT) / GAMMA_LIFT).pow(GAMMA_POWER)

    private fun lab(color: Color): Triple<Float, Float, Float> {
        val r = linear(color.red)
        val g = linear(color.green)
        val b = linear(color.blue)
        val x = curve((RX * r + RY * g + RZ * b) / WHITE_X)
        val y = curve(GX * r + GY * g + GZ * b)
        val z = curve((BX * r + BY * g + BZ * b) / WHITE_Z)
        return Triple(LAB_SCALE * y - LAB_OFFSET, LAB_A_SCALE * (x - y), LAB_B_SCALE * (y - z))
    }

    private fun curve(value: Float): Float =
        if (value > LAB_EPSILON) value.pow(1f / 3f) else LAB_KAPPA * value + LAB_OFFSET / LAB_SCALE

    private const val OFFSET = 0.05f
    private const val RED_WEIGHT = 0.2126f
    private const val GREEN_WEIGHT = 0.7152f
    private const val BLUE_WEIGHT = 0.0722f

    private const val GAMMA_KNEE = 0.03928f
    private const val GAMMA_LINEAR = 12.92f
    private const val GAMMA_LIFT = 1.055f
    private const val GAMMA_POWER = 2.4f
    private const val GAMMA_SHIFT = 0.055f

    private const val WHITE_X = 0.95047f
    private const val WHITE_Z = 1.08883f
    private const val LAB_SCALE = 116f
    private const val LAB_OFFSET = 16f
    private const val LAB_A_SCALE = 500f
    private const val LAB_B_SCALE = 200f
    private const val LAB_EPSILON = 0.008856f
    private const val LAB_KAPPA = 7.787f

    private const val RX = 0.4124564f
    private const val RY = 0.3575761f
    private const val RZ = 0.1804375f
    private const val GX = 0.2126729f
    private const val GY = 0.7151522f
    private const val GZ = 0.0721750f
    private const val BX = 0.0193339f
    private const val BY = 0.1191920f
    private const val BZ = 0.9503041f
}
