package kz.mybrain.superkassa.presentation.strings.shell

import kz.mybrain.superkassa.domain.kassa.model.StartRefusal
import kz.mybrain.superkassa.presentation.strings.common.Language

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
) {
    /** Что случилось и что делать — по причине. */
    fun of(refusal: StartRefusal): StartWords = when (refusal) {
        StartRefusal.NodeRunning -> nodeRunning
        StartRefusal.KassaRunning -> kassaRunning
        StartRefusal.BothDatabases -> bothDatabases
        StartRefusal.NodeDataUnfit -> nodeDataUnfit
        StartRefusal.KassaNotOpened -> kassaNotOpened
    }
}

/**
 * Одна причина словами кассира.
 *
 * @property reason что случилось.
 * @property action что сделать сейчас.
 */
data class StartWords(val reason: String, val action: String)

/** Надписи экрана запуска на выбранном языке. */
fun startTexts(language: Language): StartTexts = when (language) {
    Language.Kk -> startTextsKk
    Language.Ru -> startTextsRu
    Language.En -> startTextsEn
}
