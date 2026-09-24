package kz.mybrain.superkassa.domain.kassa.model.refund

import kz.mybrain.superkassa.domain.kassa.model.Tenge

/** Что не так с введённой суммой возврата — на языке кассира, а не разбора. */
enum class RefundProblem { Empty, NotANumber, NotPositive, TooLarge }

/** Проверенная сумма возврата: либо тиыны, либо причина отказа. */
sealed interface RefundAmount {
    data class Ready(val tiyn: Long) : RefundAmount
    data class Rejected(val reason: RefundProblem) : RefundAmount
}

/**
 * Проверяет сумму возврата против суммы чека-основания.
 *
 * Возврат части чека — обычное дело: покупатель вернул один товар из трёх.
 * Больше, чем было в чеке, вернуть нельзя, и узнавать об этом из отказа
 * ОФД кассир не должен — проверка идёт до отправки.
 */
internal fun refundAmountOf(entered: String, basisTiyn: Long): RefundAmount {
    val tiyn = Tenge.parse(entered)
    val problem = when {
        entered.isBlank() -> RefundProblem.Empty
        tiyn == null -> RefundProblem.NotANumber
        tiyn <= 0L -> RefundProblem.NotPositive
        tiyn > basisTiyn -> RefundProblem.TooLarge
        else -> null
    }
    return if (problem == null && tiyn != null) {
        RefundAmount.Ready(tiyn)
    } else {
        RefundAmount.Rejected(problem ?: RefundProblem.NotANumber)
    }
}

/**
 * Наличных в ящике меньше, чем возвращают деньгами.
 *
 * Возврат продажи отдаёт деньги из того же ящика, из которого их изымают,
 * и о нехватке кассир должен узнать до того, как назовёт сумму покупателю.
 * Возврат покупки деньги принимает — ему хватает всегда. Неизвестный
 * остаток молчит: утверждать нехватку по неизвестному числу нельзя.
 *
 * @return остаток ящика, когда его не хватает, иначе `null`.
 */
fun drawerShortage(kind: ReturnKind, drawerTiyn: Long?, cashRefund: Long): Long? =
    drawerTiyn?.takeIf { kind == ReturnKind.Sell && cashRefund > it }
