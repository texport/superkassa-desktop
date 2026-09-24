package kz.mybrain.superkassa.presentation.cabinet.places

import kotlin.test.Test
import kotlin.test.assertEquals

/** Названия точек идут по казахскому алфавиту, а числа в них — числами. */
class NameOrderTest {

    @Test
    fun `казахские буквы стоят на своих местах, а не после я`() {
        val names = listOf("Ясли «Бөбек»", "Қаракөз", "Магазин", "Әсем", "Автозапчасти «Нұр»", "Калина", "Ұлан")
        assertEquals(
            listOf("Автозапчасти «Нұр»", "Әсем", "Калина", "Қаракөз", "Магазин", "Ұлан", "Ясли «Бөбек»"),
            names.sortedWith(NameOrder)
        )
    }

    @Test
    fun `числа сравниваются числами`() {
        val names = listOf("Магазин на Абая 10", "Магазин на Абая 2", "Магазин на Абая 1", "Магазин на Абая 100")
        assertEquals(
            listOf("Магазин на Абая 1", "Магазин на Абая 2", "Магазин на Абая 10", "Магазин на Абая 100"),
            names.sortedWith(NameOrder)
        )
    }

    @Test
    fun `регистр букв порядок не меняет`() {
        assertEquals(0, NameOrder.compare("магазин 7", "Магазин 7"))
        assertEquals(listOf("алма", "Бота"), listOf("Бота", "алма").sortedWith(NameOrder))
    }
}
