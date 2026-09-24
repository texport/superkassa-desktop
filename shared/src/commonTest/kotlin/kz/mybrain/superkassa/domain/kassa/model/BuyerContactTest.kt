package kz.mybrain.superkassa.domain.kassa.model

import io.github.texport.superkassa.core.presentation.api.model.receipt.CustomerContactRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Контакт покупателя: разбор набранного и контакт для кассы. */
class BuyerContactTest {

    private fun phone(text: String) = BuyerContact(ContactKind.Phone, text)

    @Test
    fun `телефон Казахстана принимается так, как его диктуют`() {
        listOf("+7 701 765 43 21", "8 (701) 765-43-21", "87017654321", "77017654321", "7017654321", "+77017654321")
            .forEach { assertEquals("+77017654321", phone(it).normalized, "не разобран «$it»") }
    }

    @Test
    fun `чужой, короткий и испорченный номер не принимается`() {
        listOf("8 701 000", "+8 701 765 43 21", "+49 170 1234567", "89161234567", "+7 701 765 43 2x", "+7017654321")
            .forEach { assertTrue(phone(it).malformed, "принят «$it»") }
    }

    @Test
    fun `почта и чат Telegram разбираются по своему виду`() {
        assertEquals("buyer@example.kz", BuyerContact(ContactKind.Email, " buyer@example.kz ").normalized)
        assertTrue(BuyerContact(ContactKind.Email, "buyer@example").malformed)
        assertTrue(BuyerContact(ContactKind.Email, "buyer example.kz").malformed)
        assertEquals("123456789", BuyerContact(ContactKind.Telegram, "123456789").normalized)
        val byName = BuyerContact(ContactKind.Telegram, "@buyer")
        assertTrue(byName.malformed, "бот не пишет по имени — только по номеру чата")
    }

    @Test
    fun `пустой контакт не ошибка — чек просто не уходит покупателю`() {
        val empty = BuyerContact(ContactKind.Email, "  ")
        assertTrue(empty.empty)
        assertFalse(empty.malformed)
        assertNull(empty.toRequest())
    }

    @Test
    fun `касса получает контакт только нужного вида`() {
        assertEquals(CustomerContactRequest(phone = "+77017654321"), phone("8 701 765 43 21").toRequest())
        assertEquals(CustomerContactRequest(email = "a@b.kz"), BuyerContact(ContactKind.Email, "a@b.kz").toRequest())
        val chat = BuyerContact(ContactKind.Telegram, "5550001")
        assertEquals(CustomerContactRequest(telegram = "5550001"), chat.toRequest())
        assertNull(phone("8 701").toRequest(), "неразобранный контакт ушёл бы в кассу")
    }

    @Test
    fun `смена вида оставляет набранное и разбирает его по-новому`() {
        val typed = phone("7017654321").switchTo(ContactKind.Telegram)
        assertEquals("7017654321", typed.text)
        assertEquals("7017654321", typed.normalized)
    }

    @Test
    fun `контакт не раскрывается в журнале`() {
        assertFalse("7654321" in phone("+77017654321").toString())
    }
}
