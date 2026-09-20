package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.ui.payment.unsupportedNoteVisible
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Пояснение о погасших видах оплаты.
 *
 * Оно объясняет погасшие строки списка, а не выбранный вид. Пока условия
 * не было, касса писала «протокол 2.0.4 его не принимает» под наличными,
 * и кассир читал это как отказ принять деньги.
 */
class PaymentNoteTest {

    private val note = "погасшие виды оплаты протокол 2.0.4 не принимает"

    @Test
    fun `пояснения нет, когда все виды принимаются`() {
        assertFalse(unsupportedNoteVisible(note, listOf(entry("CASH"), entry("CARD"))))
    }

    @Test
    fun `пояснение есть, когда в списке погасший вид`() {
        assertTrue(unsupportedNoteVisible(note, listOf(entry("CASH"), entry("CREDIT", supported = false))))
    }

    @Test
    fun `пустая фраза не показывается никогда`() {
        assertFalse(unsupportedNoteVisible("", listOf(entry("TARE", supported = false))))
    }

    private fun entry(code: String, supported: Boolean = true) =
        DictionaryEntry(code = code, supported = supported)
}
