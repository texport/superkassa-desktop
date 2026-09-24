package kz.mybrain.superkassa.presentation.common.adaptive

import androidx.compose.ui.unit.dp
import kz.mybrain.superkassa.RenderProbe
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Класс окна по Material 3.
 *
 * Пороги проверяются с обеих сторон: нижняя граница класса входит в него,
 * точка перед ней — ещё в предыдущий. Ошибка на единицу здесь незаметна
 * глазом и ломает раскладку ровно на том мониторе, которого нет под рукой.
 */
class WindowClassTest {

    @Test
    fun `ширина делится на пять классов по порогам Material 3`() {
        val expected = listOf(
            0 to WidthClass.Compact,
            599 to WidthClass.Compact,
            600 to WidthClass.Medium,
            839 to WidthClass.Medium,
            840 to WidthClass.Expanded,
            1199 to WidthClass.Expanded,
            1200 to WidthClass.Large,
            1599 to WidthClass.Large,
            1600 to WidthClass.ExtraLarge,
            2560 to WidthClass.ExtraLarge
        )
        expected.forEach { (width, cls) ->
            assertEquals(cls, WindowClass.widthClassOf(width.dp), "ширина $width")
        }
    }

    @Test
    fun `высота делится на три класса по порогам Material 3`() {
        val expected = listOf(
            0 to HeightClass.Compact,
            479 to HeightClass.Compact,
            480 to HeightClass.Medium,
            899 to HeightClass.Medium,
            900 to HeightClass.Expanded,
            1440 to HeightClass.Expanded
        )
        expected.forEach { (height, cls) ->
            assertEquals(cls, WindowClass.heightClassOf(height.dp), "высота $height")
        }
    }

    /**
     * Корень окна сообщает экрану класс по настоящему размеру окна.
     *
     * Размеры — те, на которых проверялась вёрстка: наименьшее окно,
     * окно по умолчанию, два монитора и планшет стоймя и лёжа.
     */
    @Test
    fun `корень окна сообщает класс по размеру окна`() {
        val expected = mapOf(
            (960 to 640) to WindowClass(WidthClass.Expanded, HeightClass.Medium),
            (1180 to 820) to WindowClass(WidthClass.Expanded, HeightClass.Medium),
            (1920 to 1080) to WindowClass(WidthClass.ExtraLarge, HeightClass.Expanded),
            (2560 to 1080) to WindowClass(WidthClass.ExtraLarge, HeightClass.Expanded),
            (800 to 1280) to WindowClass(WidthClass.Medium, HeightClass.Expanded),
            (1280 to 800) to WindowClass(WidthClass.Large, HeightClass.Medium),
            (560 to 400) to WindowClass(WidthClass.Compact, HeightClass.Compact)
        )
        expected.forEach { (size, cls) ->
            var seen: WindowClass? = null
            RenderProbe(width = size.first, height = size.second) { seen = LocalWindowClass.current }
                .use { it.frame() }
            assertEquals(cls, seen, "окно ${size.first}×${size.second}")
        }
    }
}
