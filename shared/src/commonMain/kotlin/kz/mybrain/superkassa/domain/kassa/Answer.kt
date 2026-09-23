package kz.mybrain.superkassa.domain.kassa

import io.github.texport.superkassa.core.domain.api.exception.SuperkassaException
import io.github.texport.superkassa.core.presentation.api.SuperkassaApi
import kotlinx.coroutines.CancellationException

/**
 * Чем кончилось обращение к кассе.
 *
 * Отказ и сбой разделены, потому что кассир действует по-разному: отказ
 * по существу исправляют — другим пином, открытой сменой, — а сбой
 * кассы исправить нечем, кроме обслуживания.
 */
sealed interface Answer<out T> {

    /** Касса выполнила обращение. */
    data class Done<out T>(val value: T) : Answer<T>

    /**
     * Касса отказала по существу: код и её слова на трёх языках.
     *
     * Слова идут кассиру как есть — ядро говорит на языках Казахстана,
     * и пересказывать его своими словами значит расходиться с ним.
     */
    data class Refused(val code: String, val ru: String, val kk: String, val en: String) : Answer<Nothing>

    /**
     * Касса не смогла ответить: база, файлы, внутренняя ошибка.
     *
     * @property reason имя исключения — для журнала, не для кассира.
     */
    data class Failed(val reason: String) : Answer<Nothing>
}

/**
 * Обращается к кассе и называет итог, а не бросает его исключением.
 *
 * Отмена корутины итогом не считается: экран закрыли, не дождавшись
 * ответа, и это не отказ кассы.
 */
suspend fun <T> Kassa.ask(request: (SuperkassaApi) -> T): Answer<T> {
    val outcome = runCatching { call(request) }
    val failure = outcome.exceptionOrNull() ?: return Answer.Done(outcome.getOrThrow())
    return when (failure) {
        // Отмена — не итог, а нехватка памяти и прочие ошибки машины —
        // не сбой кассы, который кассир может переждать.
        is CancellationException, is Error -> throw failure
        is SuperkassaException -> failure.trilingualMessage.let { Answer.Refused(failure.code, it.ru, it.kk, it.en) }
        else -> Answer.Failed(failure::class.simpleName ?: UNNAMED)
    }
}

/** Имя сбоя, у исключения которого имени нет: анонимный класс. */
private const val UNNAMED = "anonymous"
