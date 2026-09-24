package kz.mybrain.superkassa.presentation.kassa.sale

import kz.mybrain.superkassa.domain.kassa.model.ContactKind
import kz.mybrain.superkassa.kassa.CoreDesk
import kz.mybrain.superkassa.presentation.common.message.Message
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Продажа на настоящем ядре, у которого доставка чека не настроена вовсе.
 *
 * Выбирать нечего: экран вместо выбора пишет одну строку, контакт остаётся
 * «не отправлять», а чек пробивается как обычно — его показывают или печатают.
 */
class SaleNoChannelsCoreTest {
    private val desk = CoreDesk()

    @AfterTest
    fun close() = desk.close()

    @Test
    fun `без каналов выбирать нечего, а чек пробивается`() {
        desk.seated()
        val model = desk.sale()
        model.add("Кумыс", "600")

        model.form.contact.kind(ContactKind.Phone)
        val state = model.state.value
        assertTrue(state.channels.none, "каналы доставки нашлись там, где их не настраивали")
        assertEquals(ContactKind.None, state.form.contact.kind, "без каналов выбрался телефон")

        model.issue()
        assertIs<Message.Done>(desk.said, desk.saidText)
    }
}
