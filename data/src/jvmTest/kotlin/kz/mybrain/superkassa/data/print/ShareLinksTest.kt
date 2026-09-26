package kz.mybrain.superkassa.data.print

import kz.mybrain.superkassa.domain.print.model.ShareWay
import kz.mybrain.superkassa.domain.print.model.SharedReceipt
import java.net.URLEncoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Ссылки мессенджеров и почты на компьютере: слова и ссылка чека закодированы,
 * пробел — `%20`, а не `+`, который почтовая программа оставила бы плюсом.
 */
class ShareLinksTest {
    private val receipt = SharedReceipt(
        bytes = byteArrayOf(1),
        name = "receipt.pdf",
        mime = "application/pdf",
        subject = "Электронный чек",
        text = "Чек: $LINK",
        link = LINK
    )

    @Test
    fun `WhatsApp получает слова со ссылкой`() {
        val expected = "https://wa.me/?text=%D0%A7%D0%B5%D0%BA%3A%20${encoded(LINK)}"
        assertEquals(expected, ShareLinks.of(receipt, ShareWay.WhatsApp))
    }

    @Test
    fun `Telegram получает ссылку отдельно, а без ссылки не открывается`() {
        val telegram = ShareLinks.of(receipt, ShareWay.Telegram).orEmpty()
        assertEquals(true, telegram.startsWith("https://t.me/share/url?url=${encoded(LINK)}&text="))
        val unlinked = SharedReceipt(receipt.bytes, "r.pdf", "application/pdf", "s", "t", link = null)
        assertNull(ShareLinks.of(unlinked, ShareWay.Telegram))
    }

    @Test
    fun `письмо — тема и тело без плюсов вместо пробелов`() {
        val mail = ShareLinks.of(receipt, ShareWay.Email).orEmpty()
        assertEquals(true, mail.startsWith("mailto:?subject="))
        assertEquals(false, "+" in mail, "пробел закодирован плюсом: $mail")
    }

    private fun encoded(text: String) = URLEncoder.encode(text, Charsets.UTF_8)

    private companion object {
        const val LINK = "https://receipt.ecc.kz/check/000000000001"
    }
}
