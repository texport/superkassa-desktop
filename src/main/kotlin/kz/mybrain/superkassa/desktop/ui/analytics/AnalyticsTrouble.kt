package kz.mybrain.superkassa.desktop.ui.analytics

import kotlinx.coroutines.CancellationException
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRefusal
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts

/**
 * Почему аналитики сейчас нет на экране.
 *
 * Три случая, и путать их нельзя. Кабинет молчит — дело в связи или
 * в адресе кабинета. Кабинет отвечает «такого не знаю» — раздел ещё
 * не выложен, и владельцу здесь чинить нечего. Кабинет отказал по
 * существу — отказ показывается его же словами.
 */
sealed interface AnalyticsTrouble {

    /** Ручки в кабинете ещё нет: выкладка не прошла. */
    data object NotDeployed : AnalyticsTrouble

    /** Кабинет не ответил вовсе. */
    data object Unreachable : AnalyticsTrouble

    /** Кабинет ответил отказом по существу. */
    data class Refused(val text: String) : AnalyticsTrouble
}

/**
 * Помеха по тому, чем кончилось обращение.
 *
 * `404` здесь не «не найдено», а «раздела ещё нет»: своих ресурсов,
 * которых можно не найти, у аналитики нет — она отвечает пустыми
 * списками и нулями даже по кассе без единого обмена.
 */
fun analyticsTrouble(failure: Throwable): AnalyticsTrouble = when {
    failure is CabinetRefusal && failure.httpStatus == NOT_FOUND -> AnalyticsTrouble.NotDeployed
    failure is CabinetRefusal -> AnalyticsTrouble.Refused(failure.text)
    // Имя исключения и его текст владельцу ничего не объясняют: на экране
    // остаётся «кабинет не отвечает», а подробности видны в журнале
    // приложения, куда обмен пишется целиком.
    else -> AnalyticsTrouble.Unreachable
}

/** Заголовок помехи словами владельца. */
fun troubleTitle(trouble: AnalyticsTrouble, texts: AnalyticsTexts): String = when (trouble) {
    AnalyticsTrouble.NotDeployed -> texts.notDeployed
    AnalyticsTrouble.Unreachable -> texts.unreachable
    is AnalyticsTrouble.Refused -> texts.refused
}

/** Что делать с помехой: объяснение под заголовком. */
fun troubleHint(trouble: AnalyticsTrouble, texts: AnalyticsTexts): String = when (trouble) {
    AnalyticsTrouble.NotDeployed -> texts.notDeployedHint
    AnalyticsTrouble.Unreachable -> texts.unreachableHint
    is AnalyticsTrouble.Refused -> trouble.text
}

/**
 * Обращение к кабинету, из которого экран не выпадает исключением.
 *
 * Отмена помехой не считается и уходит дальше: закрытый раздел или
 * сменённый источник положения не должны оставлять за собой надпись
 * «кабинет не отвечает».
 */
suspend fun <T> askedCabinet(block: suspend () -> T): Result<T> {
    val answer = runCatching { block() }
    val failure = answer.exceptionOrNull()
    if (failure is CancellationException) throw failure
    return answer
}

private const val NOT_FOUND = 404
