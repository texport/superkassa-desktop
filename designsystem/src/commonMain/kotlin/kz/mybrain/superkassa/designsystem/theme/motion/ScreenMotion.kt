package kz.mybrain.superkassa.designsystem.theme.motion

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * Переходы между экранами — по Material 3 (Motion → Transition patterns).
 *
 * Прежде экраны сменялись долгим наплывом одного на другой, и уходящий
 * экран был виден сквозь новый. По Material 3 переходов два:
 * - между разделами одного уровня — «fade through»: уходящий гаснет
 *   быстро, и лишь потом приходящий проявляется с лёгким увеличением;
 *   двух экранов сразу не видно;
 * - вглубь и обратно — «shared axis X»: экраны сдвигаются вдоль оси
 *   на несколько десятков точек и сменяются так же без наплыва; «назад»
 *   двигает их в обратную сторону.
 */
class ScreenMotion internal constructor(private val shift: Int) {

    /** Между разделами одного уровня: «fade through». */
    fun fadeThrough(): ContentTransform {
        val enter = tween<Float>(ENTER_MS, delayMillis = EXIT_MS, easing = Decelerate)
        return (fadeIn(enter) + scaleIn(enter, initialScale = ENTER_SCALE)) togetherWith
            fadeOut(tween(EXIT_MS, easing = Accelerate))
    }

    /** Шаг вглубь: приходящий въезжает справа. */
    fun forward(): ContentTransform = axis(FORWARD)

    /** Шаг назад: приходящий въезжает слева. */
    fun backward(): ContentTransform = axis(-FORWARD)

    private fun axis(direction: Int): ContentTransform {
        val slide = tween<IntOffset>(AXIS_MS, easing = Standard)
        val enter = slideInHorizontally(slide) { direction * shift } +
            fadeIn(tween(ENTER_MS, delayMillis = EXIT_MS, easing = Decelerate))
        val exit = slideOutHorizontally(slide) { -direction * shift } + fadeOut(tween(EXIT_MS, easing = Accelerate))
        return enter togetherWith exit
    }

    private companion object {
        /** Уходящий гаснет — 90 мс по Material 3. */
        const val EXIT_MS = 90

        /** Приходящий проявляется после ухода — 210 мс. */
        const val ENTER_MS = 210

        /** Сдвиг вдоль оси — весь переход, 300 мс. */
        const val AXIS_MS = 300

        /** С какого масштаба проявляется раздел в «fade through». */
        const val ENTER_SCALE = 0.92f

        const val FORWARD = 1

        /** Кривые Material 3: стандартная, выразительные замедление и ускорение. */
        val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val Decelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
        val Accelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    }
}

/** Сдвиг экранов вдоль оси в «shared axis» — 30 точек по Material 3. */
private val AxisShift = 30.dp

/** Переходы окна: сдвиг вдоль оси — в точках этого экрана. */
@Composable
fun rememberScreenMotion(): ScreenMotion {
    val shift = with(LocalDensity.current) { AxisShift.roundToPx() }
    return remember(shift) { ScreenMotion(shift) }
}
