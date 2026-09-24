package kz.mybrain.superkassa.presentation.cabinet

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kz.mybrain.superkassa.presentation.common.model.Busy
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.strings.cabinet.cabinetTexts

/** Итог обращения к кабинету: значение или помеха — прямо в руки вызвавшему. */
sealed interface CabinetReply<out T> {
    data class Done<out T>(val value: T) : CabinetReply<T>

    data class Failed(val problem: CabinetProblem) : CabinetReply<Nothing>
}

/** Значение удавшегося обращения; `null` — не удалось, помеха уже показана. */
val <T> CabinetReply<T>.value: T? get() = (this as? CabinetReply.Done)?.value

/** Помеха неудавшегося обращения; `null` — удалось. */
val CabinetReply<*>.problem: CabinetProblem? get() = (this as? CabinetReply.Failed)?.problem

/**
 * Обращения к кабинету одного окна: одна занятость и один разбор помех.
 *
 * Занятость — общий счётчик моделей [Busy]: пока идёт подача заявления,
 * карточка кассы опрашивает кабинет, и первое же завершившееся обращение
 * не гасит занятость посреди подписи. Отказ показывается и снимается
 * по общему правилу строки сообщений [Talk]: удача снимает только отказ
 * того же обращения.
 *
 * Итог обращения отдаётся вызвавшему, а не кладётся в общее поле: прежде
 * экран читал помеху из общего поля, и опрос соседней карточки успевал стереть
 * её раньше, чем экран её прочёл.
 *
 * Опрос идёт [quiet]: он не занимает окно, не пишет в строку сообщений
 * и не снимает показанный отказ — владелец не просил его и не должен
 * терять из-за него то, что прочёл.
 */
class CabinetWork(private val talk: Talk) {
    private val busy = Busy()

    /** Сколько обращений, начатых владельцем, идёт сейчас. */
    val running: StateFlow<Int> = busy.running

    /**
     * Обращение, которого ждёт владелец.
     *
     * Помеха показывается в строке сообщений окна и возвращается вызвавшему.
     * Удача снимает отказ, который вызвало то же обращение, если он ещё
     * на экране: то, что не удалось, теперь удалось, и отказ уже неправда.
     * Удача соседнего обращения его не трогает: чтение состояния, прошедшее
     * после упавшего чтения карточки, о карточке ничего не говорит. Чужие
     * сообщения удача не трогает тем более: отказ кассы никто не отменял.
     *
     * @param action что делалось, по-английски: для журнала; оно же
     *   отличает одно обращение от другого.
     */
    suspend fun <T> run(action: String, block: suspend () -> T): CabinetReply<T> {
        val outcome = busy.during { runCatching { block() } }
        val failure = outcome.exceptionOrNull()
        if (failure == null) {
            talk.succeeded(action)
            return CabinetReply.Done(outcome.getOrThrow())
        }
        return CabinetReply.Failed(show(action, rethrown(failure)))
    }

    /**
     * Обращение, о котором владельцу знать незачем: справочное наименование,
     * опрос ответа ИСНА. Отказ уходит только в журнал.
     */
    suspend fun <T> quiet(action: String, block: suspend () -> T): T? {
        val outcome = runCatching { block() }
        val failure = outcome.exceptionOrNull() ?: return outcome.getOrThrow()
        talk.journal.warn("cabinet: $action ${cabinetProblemOf(rethrown(failure)).logged()}")
        return null
    }

    private fun show(action: String, failure: Throwable): CabinetProblem {
        val problem = cabinetProblemOf(failure)
        talk.journal.failure("cabinet: $action ${problem.logged()}")
        talk.say(action, cabinetMessage(problem, cabinetTexts(talk.language())))
        return problem
    }

    /** Отмена — не помеха, а ошибки машины — не отказ кабинета: их пропускаем дальше. */
    private fun rethrown(failure: Throwable): Throwable {
        if (failure is CancellationException || failure is Error) throw failure
        return failure
    }
}
