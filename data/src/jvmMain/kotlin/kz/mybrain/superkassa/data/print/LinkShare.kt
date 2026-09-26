package kz.mybrain.superkassa.data.print

import kz.mybrain.superkassa.data.local.openInBrowser
import kz.mybrain.superkassa.data.local.openMail
import kz.mybrain.superkassa.domain.print.model.ShareWay
import kz.mybrain.superkassa.domain.print.model.SharedReceipt
import kz.mybrain.superkassa.domain.print.port.ShareOut
import java.net.URLEncoder

/**
 * Поделиться чеком на компьютере — ссылками WhatsApp, Telegram и почты.
 *
 * Окна «Поделиться» у настольной системы нет, а мессенджеры и почта
 * открываются ссылкой: `wa.me` и `t.me` ведут в установленное приложение
 * или в его веб-версию, `mailto:` — в почтовую программу. Файл ссылкой
 * не передать, поэтому уходит ссылка на электронный чек; файл владелец
 * сохраняет кнопкой «Сохранить» и прикладывает сам.
 */
class LinkShare : ShareOut {

    override val ways: List<ShareWay> = listOf(ShareWay.WhatsApp, ShareWay.Telegram, ShareWay.Email)

    override suspend fun share(receipt: SharedReceipt, way: ShareWay): Boolean {
        val address = ShareLinks.of(receipt, way) ?: return false
        return if (way == ShareWay.Email) openMail(address) else openInBrowser(address)
    }
}

/** Адреса мессенджеров и почты со словами и ссылкой чека. */
internal object ShareLinks {

    /** Адрес для пути [way]; `null` — путь ссылкой не открывается. */
    fun of(receipt: SharedReceipt, way: ShareWay): String? = when (way) {
        ShareWay.WhatsApp -> "https://wa.me/?text=${encode(receipt.text)}"
        ShareWay.Telegram -> receipt.link?.let { link ->
            "https://t.me/share/url?url=${encode(link)}&text=${encode(receipt.subject)}"
        }
        ShareWay.Email -> "mailto:?subject=${encode(receipt.subject)}&body=${encode(receipt.text)}"
        ShareWay.System -> null
    }

    /** Пробел — `%20`, а не `+`: почтовые программы плюс оставляют плюсом. */
    private fun encode(text: String): String = URLEncoder.encode(text, Charsets.UTF_8).replace("+", "%20")
}
