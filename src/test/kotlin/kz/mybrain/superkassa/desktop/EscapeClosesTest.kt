package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.components.EscapeCloses
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Escape закрывает верхнее из открытых наложений.
 *
 * Наложения лежат друг на друге, и клавиша должна снимать последнее
 * открытое: диалог над печатной формой, а не форму под ним. Когда
 * закрывать нечего, нажатие не считается обработанным — окно отдаёт
 * его дальше.
 */
class EscapeClosesTest {

    @Test
    fun `закрывается последнее открытое, затем то, что под ним`() {
        val closed = mutableListOf<String>()
        val form: () -> Unit = { closed += "form" }
        val dialog: () -> Unit = { closed += "dialog" }
        EscapeCloses.register(form)
        EscapeCloses.register(dialog)
        try {
            assertTrue(EscapeCloses.press())
            EscapeCloses.unregister(dialog)
            assertTrue(EscapeCloses.press())
            assertEquals(listOf("dialog", "form"), closed)
        } finally {
            EscapeCloses.unregister(form)
            EscapeCloses.unregister(dialog)
        }
    }

    @Test
    fun `без наложений нажатие не обработано`() {
        assertFalse(EscapeCloses.press())
    }
}
