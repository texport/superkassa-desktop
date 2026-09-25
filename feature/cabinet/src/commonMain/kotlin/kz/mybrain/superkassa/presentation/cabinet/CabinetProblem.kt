package kz.mybrain.superkassa.presentation.cabinet

import kz.mybrain.superkassa.domain.cabinet.model.CabinetExpired
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRefusal
import kz.mybrain.superkassa.domain.cabinet.model.CabinetUnreachable
import kz.mybrain.superkassa.domain.cabinet.model.CabinetUnreadable
import kz.mybrain.superkassa.domain.cabinet.model.EdsProblem
import kz.mybrain.superkassa.domain.cabinet.model.EdsRefusal

/**
 * Помеха в кабинете: что именно не получилось.
 *
 * Отказ кабинета по существу, его недоступность и отказ подписи приходят
 * разными исключениями из разных портов, а владельцу показываются одной
 * строкой. Разбор, кто есть кто, собран здесь целиком — рядом с перечнем.
 */
sealed interface CabinetProblem {
    /** Кабинет ответил отказом по существу. */
    data class Refused(val code: String, val text: String) : CabinetProblem

    /** Кабинет не отвечает по заданному адресу. */
    data class Unreachable(val reason: String) : CabinetProblem

    /**
     * Кабинет ответил, но прочитать ответ нечем.
     *
     * Так выглядит разошедшийся договор: поле сменило имя или тип. Это
     * не молчание службы, и говорить о нём словами молчания нельзя —
     * владелец идёт проверять сеть и доступ к службе, которая отвечает.
     */
    data object Unreadable : CabinetProblem

    /** Подписывающий (NCALayer) не запущен на этой машине. */
    data object NoNcaLayer : CabinetProblem

    /** Владелец не подписал: закрыл окно или ошибся паролем. */
    data class SignDeclined(val detail: String) : CabinetProblem

    /** Доступ истёк — нужно войти заново. */
    data object SessionExpired : CabinetProblem

    companion object {
        const val MAX_REASON = 300
    }
}

/**
 * Какая помеха стоит за исключением порта кабинета.
 *
 * Всё незнакомое — недоступность: владельцу — имя причины, подробности —
 * в журнал. Прежде на экран попадал текст ошибки разбора ответа целиком,
 * и вместо объяснения стояло «Unexpected JSON token at offset 0».
 */
internal fun cabinetProblemOf(failure: Throwable): CabinetProblem = when (failure) {
    is CabinetExpired -> CabinetProblem.SessionExpired
    is CabinetRefusal -> CabinetProblem.Refused(failure.code, failure.text)
    is EdsRefusal -> when (failure.problem) {
        EdsProblem.Unreachable -> CabinetProblem.NoNcaLayer
        EdsProblem.Declined -> CabinetProblem.SignDeclined(failure.detail)
    }
    is CabinetUnreadable -> CabinetProblem.Unreadable
    is CabinetUnreachable -> CabinetProblem.Unreachable(failure.reason.take(CabinetProblem.MAX_REASON))
    else -> CabinetProblem.Unreachable(failure::class.simpleName.orEmpty().take(CabinetProblem.MAX_REASON))
}

/** Строка журнала о помехе: по-английски и без содержимого ответа. */
internal fun CabinetProblem.logged(): String = when (this) {
    is CabinetProblem.Refused -> "refused $code"
    is CabinetProblem.Unreachable -> "unreachable $reason"
    CabinetProblem.Unreadable -> "answer not readable"
    CabinetProblem.NoNcaLayer -> "signer unreachable"
    is CabinetProblem.SignDeclined -> "signature declined"
    CabinetProblem.SessionExpired -> "access expired"
}

/** Отказал ли кабинет из-за открытой смены. */
internal fun CabinetProblem.isShiftOpen(): Boolean =
    this is CabinetProblem.Refused && code == SHIFT_IS_OPEN

/** Код, которым кабинет отвечает на действие при открытой смене. */
private const val SHIFT_IS_OPEN = "SHIFT_IS_OPEN"
