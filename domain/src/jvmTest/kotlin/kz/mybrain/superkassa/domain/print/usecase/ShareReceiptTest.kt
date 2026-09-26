package kz.mybrain.superkassa.domain.print.usecase

import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.print.model.ShareWay
import kz.mybrain.superkassa.domain.print.model.Shared
import kz.mybrain.superkassa.domain.print.model.SharedReceipt
import kz.mybrain.superkassa.domain.print.port.FakeShareOut
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Поделиться чеком: путь, по которому уходит только ссылка, без ссылки не открывается.
 *
 * Пустое сообщение в WhatsApp покупателю ничего не даст, а кассир решил бы,
 * что чек отправлен.
 */
class ShareReceiptTest {

    private fun receipt(link: String?) =
        SharedReceipt(byteArrayOf(1), "receipt.pdf", "application/pdf", "Электронный чек", "Электронный чек", link)

    @Test
    fun `мессенджер без ссылки на чек не открывается`() {
        val out = FakeShareOut(ways = listOf(ShareWay.WhatsApp))
        assertEquals(Shared.NoLink, runBlocking { ShareReceipt(out)(receipt(link = null), ShareWay.WhatsApp) })
        assertTrue(out.shared.isEmpty(), "пустое сообщение ушло в мессенджер")
    }

    @Test
    fun `окно системы отдаёт файл и без ссылки`() {
        val out = FakeShareOut()
        assertEquals(Shared.Opened, runBlocking { ShareReceipt(out)(receipt(link = null), ShareWay.System) })
        assertEquals(1, out.shared.size)
    }

    @Test
    fun `не открытое системой — неудача`() {
        val out = FakeShareOut(opens = false)
        assertEquals(Shared.Failed, runBlocking { ShareReceipt(out)(receipt(LINK), ShareWay.System) })
    }

    private companion object {
        const val LINK = "https://receipt.ecc.kz/check/000000000001"
    }
}
