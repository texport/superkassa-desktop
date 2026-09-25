package kz.mybrain.superkassa.strings.api.shell

/**
 * Надписи экрана, которым касса говорит, что не открылась.
 *
 * Своим набором: экран показывается до всех разделов, когда ни входа,
 * ни настроек ещё нет, и ни к одному из них не относится.
 */
data class ShellTexts(
    val title: String,
    val nodeRunning: StartFailureTexts,
    val kassaRunning: StartFailureTexts,
    val bothDatabases: StartFailureTexts,
    val nodeDataUnfit: StartFailureTexts,
    val kassaNotOpened: StartFailureTexts,
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
data class StartFailureTexts(val reason: String, val action: String)
