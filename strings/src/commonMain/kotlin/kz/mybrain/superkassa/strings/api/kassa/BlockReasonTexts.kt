package kz.mybrain.superkassa.strings.api.kassa

import kz.mybrain.superkassa.strings.impl.kassa.blockReasonWords

/**
 * Почему касса заблокирована — словами кассира и с тем, что делать.
 *
 * Блокировку накладывает узел, и у неё есть своя причина: отозванный
 * токен, сутки открытой смены, слишком долгая автономная работа, отказ
 * БФД. Прежде на этом месте стояла одна фраза на все случаи, и она же
 * говорила про снятие с учёта — кассир с отозванным токеном читал, что
 * его кассу сняли с учёта, и шёл не туда.
 *
 * Коды своих блокировок узел берёт из отдельного диапазона, а отказ БФД
 * переносит как есть, прибавив тысячу: так «неверный токен» (2) узла
 * не путается с его собственной причиной.
 */
data class BlockReasonTexts(
    val unknown: String,
    val invalidToken: String,
    val shiftTooLong: String,
    val autonomousTooLong: String,
    val deregistered: String,
    val disconnected: String,
    val incorrectData: String,
    val readingStays: String
) {
    /**
     * Причина блокировки по её коду.
     *
     * `null` кода или незнакомый код — общая причина: «касса заблокирована»
     * без домыслов о том, чего приложение не знает.
     */
    fun words(code: Int?): String = blockReasonWords(this, code)
}
