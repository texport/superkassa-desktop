package kz.mybrain.superkassa.strings.api.shell

/**
 * Надписи экрана, которым касса говорит, что не открылась.
 *
 * Своим набором: экран показывается до всех разделов, когда ни входа,
 * ни настроек ещё нет, и ни к одному из них не относится.
 */
data class StartTexts(
    val title: String,
    val nodeRunning: StartWords,
    val kassaRunning: StartWords,
    val bothDatabases: StartWords,
    val nodeDataUnfit: StartWords,
    val kassaNotOpened: StartWords,
    /** Подпись к сведениям для обслуживания. */
    val forSupport: String,
    val close: String
)

/**
 * Одна причина словами кассира.
 *
 * @property reason что случилось.
 * @property action что сделать сейчас.
 */
data class StartWords(val reason: String, val action: String)
