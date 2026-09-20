package kz.mybrain.superkassa.desktop.app

import kz.mybrain.superkassa.desktop.app.log.AppLog
import kz.mybrain.superkassa.desktop.app.log.LogLevel
import kz.mybrain.superkassa.desktop.app.log.LogSource
import kz.mybrain.superkassa.desktop.eds.EdsProblem
import kz.mybrain.superkassa.desktop.eds.EdsRefusal
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRefusal

/**
 * Помеха в кабинете: что именно не получилось и как это узнаётся.
 *
 * Отдельный предмет от самого сеанса: отказ кабинета по существу,
 * недоступность кабинета и отказ подписи приходят разными исключениями
 * из разных мест, а владельцу показываются одной строкой. Разбор, кто есть
 * кто, собран здесь целиком — рядом с перечнем помех, а не посреди сеанса,
 * где он занимал больше места, чем вся работа с кабинетом.
 */

/** Почему действие в кабинете не удалось. */
sealed interface CabinetProblem {
    /** Кабинет ответил отказом по существу. */
    data class Refused(val code: String, val text: String) : CabinetProblem

    /** Кабинет не отвечает по заданному адресу. */
    data class Unreachable(val reason: String) : CabinetProblem

    /** NCALayer не запущен на этой машине. */
    data object NoNcaLayer : CabinetProblem

    /** Владелец не подписал: закрыл окно или ошибся паролем. */
    data class SignDeclined(val detail: String) : CabinetProblem

    /** Доступ истёк — нужно войти заново. */
    data object SessionExpired : CabinetProblem

    companion object {
        const val UNAUTHORIZED = 401
        const val MAX_REASON = 300
    }
}

/**
 * Кончился ли этим отказом весь сеанс.
 *
 * Истёкшим доступ считается только когда он был: при входе никакого
 * доступа ещё нет, и 401 там означает отказ по подписи — сертификат
 * просрочен, корень не тот, подпись не сходится. Прятать это
 * за «войдите заново» значит предлагать повторить то, что не сработает.
 */
internal fun CabinetRefusal.endsSession(entered: Boolean): Boolean =
    httpStatus == CabinetProblem.UNAUTHORIZED && entered

/** Отказ кабинета по существу: его код и его слова. */
internal fun CabinetRefusal.asProblem(): CabinetProblem = CabinetProblem.Refused(code, text)

/** Подпись не получена: либо NCALayer не запущен, либо владелец отказался. */
internal fun EdsRefusal.asProblem(): CabinetProblem {
    AppLog.signatureRefused("подпись не получена: $problem $detail")
    return when (problem) {
        EdsProblem.Unreachable -> CabinetProblem.NoNcaLayer
        EdsProblem.Declined -> CabinetProblem.SignDeclined(detail)
    }
}

/**
 * Всё прочее: владельцу — имя причины, подробности — в журнал.
 *
 * Прежде на экран попадал текст ошибки разбора ответа целиком, и вместо
 * объяснения стояло «Unexpected JSON token at offset 0»: читать это
 * владельцу незачем, а поддержке хватит журнала.
 */
internal fun Exception.asCabinetProblem(): CabinetProblem {
    AppLog.record(
        source = LogSource.Cabinet,
        level = LogLevel.Failure,
        text = "кабинет: ${this::class.simpleName}",
        body = message
    )
    return CabinetProblem.Unreachable(this::class.simpleName.orEmpty().take(CabinetProblem.MAX_REASON))
}
