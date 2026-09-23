package kz.mybrain.superkassa.presentation.messages

import kz.mybrain.superkassa.domain.journal.Journal
import kz.mybrain.superkassa.domain.kassa.Answer
import kz.mybrain.superkassa.presentation.AppContainer
import kz.mybrain.superkassa.presentation.strings.Language

/**
 * Итог обращения к кассе — в строку сообщений и в журнал.
 *
 * Одна дорога для всех экранов: отказ говорит словами кассы на языке
 * кассира, сбой — что именно не выполнено. Журнал получает код и имя
 * сбоя, а не слова: по-английски и без содержимого документа.
 *
 * @param what что делалось, словами кассира: «Открыть смену».
 * @param action что делалось, для журнала: `open shift`.
 * @return значение ответа; `null` — касса отказала или не смогла.
 */
fun <T> Answer<T>.shown(what: String, action: String, notices: Notices, journal: Journal, language: Language): T? =
    when (this) {
        is Answer.Done -> value
        is Answer.Refused -> {
            journal.warn("$action: refused $code")
            notices.show(Message.Refusal(words(language), code))
            null
        }

        is Answer.Failed -> {
            journal.failure("$action: failed $reason")
            notices.show(Message.Failed(what))
            null
        }
    }

/** То же, с портами окна: строка сообщений, журнал и язык кассира — из [app]. */
fun <T> Answer<T>.shown(what: String, action: String, app: AppContainer): T? =
    shown(what, action, app.notices, app.journal, app.language())

/** Слова отказа на языке кассира. */
fun Answer.Refused.words(language: Language): String = when (language) {
    Language.Ru -> ru
    Language.Kk -> kk
    Language.En -> en
}
