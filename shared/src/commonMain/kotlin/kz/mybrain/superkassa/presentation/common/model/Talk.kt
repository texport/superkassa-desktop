package kz.mybrain.superkassa.presentation.common.model

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.log.port.Journal
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.common.message.words
import kz.mybrain.superkassa.strings.api.Language

/**
 * Чем модели окна говорят с кассиром: строка сообщений, журнал и язык.
 *
 * Одна на окно и одна дорога для всех областей: отказ говорит словами кассы
 * на языке кассира, сбой — что именно не выполнено, журнал получает код
 * и имя сбоя, а не слова.
 *
 * Удача снимает отказ, который вызвало то же обращение, если он ещё
 * на экране: то, что не удалось, теперь удалось, и отказ уже неправда.
 * Удача соседнего обращения его не трогает — чтение состояния, прошедшее
 * после упавшего сохранения, о сохранении ничего не говорит. Обращение
 * узнаётся по `action` — имени для журнала.
 *
 * @property language язык кассира сейчас.
 */
class Talk(val notices: Notices, val journal: Journal, val language: () -> Language) {
    private var said: Said? = null

    /**
     * Итог обращения — в строку сообщений и в журнал.
     *
     * @param what что делалось, словами кассира: «Открыть смену».
     * @param action что делалось, для журнала: `open shift`.
     * @return значение ответа; `null` — касса отказала или не смогла.
     */
    fun <T> shown(answer: Answer<T>, what: String, action: String): T? = when (answer) {
        is Answer.Done -> answer.value.also { succeeded(action) }
        is Answer.Refused -> null.also {
            journal.warn("$action: refused ${answer.code}")
            say(action, Message.Refusal(answer.words(language()), answer.code))
        }

        is Answer.Failed -> null.also {
            journal.failure("$action: failed ${answer.reason}")
            say(action, Message.Failed(what))
        }
    }

    /** Итог действия, о котором кассир спрашивал; [action] — для журнала, если действие стоит записать. */
    fun done(text: String, action: String? = null) {
        action?.let { journal.info("$it: done") }
        notices.show(Message.Done(text))
    }

    /** Отказ или сбой своими словами; [action] отличает его от чужих. */
    fun say(action: String, message: Message) {
        notices.show(message)
        said = Said(action, message)
    }

    /** Обращение [action] удалось: его прежний отказ снимается, чужие остаются. */
    fun succeeded(action: String) {
        val own = said?.takeIf { it.action == action } ?: return
        notices.clear(own.message)
        said = null
    }

    /** Снимает строку: начато новое действие, и прежний итог больше не о нём. */
    fun clear() = notices.clear()

    /** Строка сейчас пуста: сказать можно, ничего не перебив. */
    val silent: Boolean get() = notices.last == null

    /** Отказ в строке сообщений и обращение, которое его вызвало. */
    private class Said(val action: String, val message: Message)
}

/** То же, что [Talk.shown], записью от ответа. */
fun <T> Answer<T>.shown(what: String, action: String, talk: Talk): T? = talk.shown(this, what, action)
