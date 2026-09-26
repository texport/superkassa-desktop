package kz.mybrain.superkassa.strings.api.common

/**
 * Надписи «Поделиться чеком»: кнопка, пути и слова сообщения покупателю.
 *
 * Чек уходит программой, которой покупателю пишут и так, — окном
 * «Поделиться» Android или ссылкой мессенджера и почты на компьютере.
 */
data class ShareTexts(
    val share: String,
    val whatsApp: String,
    val telegram: String,
    val email: String,
    /** Заголовок письма и сообщения. */
    val subject: String,
    /** Слова сообщения со ссылкой: `%s` — ссылка на электронный чек. */
    val withLink: String,
    /** Слова сообщения без ссылки: уходит один файл. */
    val withoutLink: String,
    /** Ссылки на чек ещё нет, а этим путём уходит только она. */
    val noLink: String,
    /** Система не открыла ни окна, ни программы. */
    val failed: String
)
