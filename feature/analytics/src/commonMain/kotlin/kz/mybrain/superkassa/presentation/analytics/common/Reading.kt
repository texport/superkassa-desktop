package kz.mybrain.superkassa.presentation.analytics.common

import kz.mybrain.superkassa.domain.analytics.model.AnalyticsAnswer
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsTrouble

/**
 * Ответ кабинета на экране аналитики: что пришло, что помешало и идёт ли
 * чтение.
 *
 * Одно состояние на все вкладки: ожидание, помеха и пришедшее
 * у них читаются одинаково, и разойдись они — одна вкладка говорила бы
 * «кабинет не отвечает», а соседняя молча крутила бы ожидание.
 *
 * Перечитывание не гасит прежний ответ: пока идёт новое чтение,
 * на экране остаётся прежнее, а не пустое место.
 */
data class Reading<out T>(
    val value: T? = null,
    val trouble: AnalyticsTrouble? = null,
    val loading: Boolean = false
) {
    /** Чтение началось: прежняя помеха снимается, прежнее пришедшее остаётся. */
    fun started(): Reading<T> = copy(loading = true, trouble = null)

    companion object {
        /** Чтение кончилось: пришедшее — или помеха вместо него. */
        fun <T> of(answer: AnalyticsAnswer<T>): Reading<T> = when (answer) {
            is AnalyticsAnswer.Done -> Reading(answer.value)
            is AnalyticsAnswer.Troubled -> Reading(trouble = answer.trouble)
        }
    }
}

/**
 * Владелец кабинета, за которым следит модель вкладки.
 *
 * Вошёл другой владелец или вышел этот — прежний ответ принадлежит
 * не ему, и модель читает заново. Тот же владелец — ничего не делается:
 * возврат на вкладку не стоит владельцу нового ожидания. Сам доступ
 * модели не нужен — его держит кабинет; здесь только отметка входа.
 */
internal class OwnerAccess {

    /** Отметка вошедшего сейчас; `null` — в кабинет не входили. */
    var current: String? = null
        private set

    /** Запоминает отметку; `true` — владелец сменился. */
    fun changed(owner: String?): Boolean {
        if (owner == current) return false
        current = owner
        return true
    }
}
