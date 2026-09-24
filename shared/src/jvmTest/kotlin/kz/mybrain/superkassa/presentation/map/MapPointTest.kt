package kz.mybrain.superkassa.presentation.map

import kotlin.test.Test
import kotlin.test.assertEquals

/** Точка торговой точки уходит в кабинет в его записи градусов. */
class MapPointTest {

    @Test
    fun `координаты уходят в кабинет с шестью знаками, а не со всеми`() {
        assertEquals("43.238949", cabinetDegrees(43.2389493827).toString())
        assertEquals("76.8", cabinetDegrees(76.8000000).toString())
    }
}
