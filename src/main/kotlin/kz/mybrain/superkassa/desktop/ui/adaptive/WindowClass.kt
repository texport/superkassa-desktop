package kz.mybrain.superkassa.desktop.ui.adaptive

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.WindowBreakpoints

/** Класс ширины окна по Material 3, от узкого к широкому. */
enum class WidthClass { Compact, Medium, Expanded, Large, ExtraLarge }

/** Класс высоты окна по Material 3: у высоты их три. */
enum class HeightClass { Compact, Medium, Expanded }

/**
 * Класс окна: по нему экран решает раскладку, а не по числу точек.
 *
 * Классы упорядочены, поэтому сравниваются как числа:
 * `window.width >= WidthClass.Large`.
 */
@Immutable
data class WindowClass(val width: WidthClass, val height: HeightClass) {

    companion object {

        /** Класс окна данного размера; пороги — [WindowBreakpoints]. */
        fun of(width: Dp, height: Dp): WindowClass = WindowClass(widthClassOf(width), heightClassOf(height))

        /** Класс ширины: нижняя граница класса входит в него. */
        fun widthClassOf(width: Dp): WidthClass = with(WindowBreakpoints) {
            when {
                width >= extraLargeWidthFrom -> WidthClass.ExtraLarge
                width >= largeWidthFrom -> WidthClass.Large
                width >= expandedWidthFrom -> WidthClass.Expanded
                width >= mediumWidthFrom -> WidthClass.Medium
                else -> WidthClass.Compact
            }
        }

        /** Класс высоты: нижняя граница класса входит в него. */
        fun heightClassOf(height: Dp): HeightClass = with(WindowBreakpoints) {
            when {
                height >= expandedHeightFrom -> HeightClass.Expanded
                height >= mediumHeightFrom -> HeightClass.Medium
                else -> HeightClass.Compact
            }
        }
    }
}

/**
 * Класс окна, в котором рисуется экран.
 *
 * Без корня — класс стартового окна кассы: так экран, нарисованный
 * отдельно от окна, раскладывается как в окне по умолчанию, а не как
 * на телефоне.
 */
val LocalWindowClass = compositionLocalOf { WindowClass.of(Sizes.windowWidth, Sizes.windowHeight) }

/**
 * Корень окна: меряет его один раз и сообщает класс всему, что внутри.
 *
 * Экраны не меряют окно сами: у каждого своя ширина за вычетом рельса,
 * и один и тот же монитор оказывался бы у разных разделов разным классом.
 * Читатели [LocalWindowClass] перерисовываются только при смене класса,
 * а не на каждом движении рамки: значение — пара перечислений.
 */
@Composable
fun WindowClassRoot(content: @Composable () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val window = WindowClass.of(maxWidth, maxHeight)
        CompositionLocalProvider(LocalWindowClass provides window, content = content)
    }
}
