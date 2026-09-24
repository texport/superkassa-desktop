package kz.mybrain.superkassa.domain.analytics.model

/** Чем кончилось обращение к аналитике кабинета. */
sealed interface AnalyticsAnswer<out T> {

    /** Кабинет ответил по существу. */
    data class Done<out T>(val value: T) : AnalyticsAnswer<T>

    /** Ответа нет, и почему — сказано [trouble]. */
    data class Troubled(val trouble: AnalyticsTrouble) : AnalyticsAnswer<Nothing>
}

/**
 * Почему аналитики сейчас нет на экране.
 *
 * Случаи не путаются. Кабинет молчит — дело в связи или в адресе кабинета.
 * Кабинет отвечает «такого не знаю» — раздел ещё не выложен, и владельцу
 * здесь чинить нечего. Доступ владельца кончился — войти заново. Кабинет
 * отказал по существу — отказ показывается его же словами.
 */
sealed interface AnalyticsTrouble {

    /** Ручки в кабинете ещё нет: выкладка не прошла. */
    data object NotDeployed : AnalyticsTrouble

    /** Кабинет не ответил вовсе. */
    data object Unreachable : AnalyticsTrouble

    /** Владелец не входил в кабинет или его доступ кончился. */
    data object SignedOut : AnalyticsTrouble

    /** Кабинет ответил отказом по существу. */
    data class Refused(val text: String) : AnalyticsTrouble
}

/** Значение ответа; помеха — `null`. */
fun <T> AnalyticsAnswer<T>.valueOrNull(): T? = (this as? AnalyticsAnswer.Done)?.value

/** Помеха ответа; ответ по существу — `null`. */
fun AnalyticsAnswer<*>.troubleOrNull(): AnalyticsTrouble? = (this as? AnalyticsAnswer.Troubled)?.trouble
