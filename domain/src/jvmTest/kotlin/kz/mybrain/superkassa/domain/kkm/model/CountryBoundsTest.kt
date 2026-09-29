package kz.mybrain.superkassa.domain.kkm.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Касса стоит в Казахстане: точка вне страны — ошибка координат, а не касса за границей. */
class CountryBoundsTest {

    @Test
    fun `города страны в её пределах`() {
        assertTrue(CountryBounds.contains(43.238949, 76.889709), "Алматы")
        assertTrue(CountryBounds.contains(51.089753, 71.406166), "Астана")
        assertTrue(CountryBounds.contains(47.106944, 51.903611), "Атырау")
        assertTrue(CountryBounds.contains(49.948056, 82.627778), "Усть-Каменогорск")
    }

    @Test
    fun `точки за границей страны вне её`() {
        assertFalse(CountryBounds.contains(52.520008, 13.404954), "Берлин")
        assertFalse(CountryBounds.contains(41.311081, 69.240562 - 30), "запад за Каспием")
        assertFalse(CountryBounds.contains(56.0, 70.0), "севернее страны")
    }

    @Test
    fun `края прямоугольника входят в него`() {
        assertTrue(CountryBounds.contains(40.5, 46.4))
        assertTrue(CountryBounds.contains(55.5, 87.4))
        assertFalse(CountryBounds.contains(40.49, 60.0))
    }
}
