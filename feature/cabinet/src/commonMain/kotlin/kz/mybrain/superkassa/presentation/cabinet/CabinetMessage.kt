package kz.mybrain.superkassa.presentation.cabinet

import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.domain.cabinet.model.cancelledBySigner
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Помеха кабинета — сообщением рабочего места.
 *
 * Сообщение в приложении одно: всплывающая строка внизу окна. Прежде
 * кабинет писал свои помехи красной строкой посреди раздела — она
 * оставалась висеть после того, как всё исправлено, терялась при
 * прокрутке и говорила по-английски то, что ответил сервер.
 *
 * Отказ по существу переводится по коду: кабинет отвечает кодом и
 * английским пояснением для поддержки, а владельцу нужно то же самое
 * его словами. Незнакомый код доходит с пояснением сервера — молчать
 * об отказе хуже, чем сказать о нём чужими словами.
 */
internal fun cabinetMessage(problem: CabinetProblem, texts: CabinetTexts): Message = when (problem) {
    is CabinetProblem.Refused -> Message.Refusal(refusalWords(problem.code, texts) ?: problem.text, problem.code)
    // Молчит кабинет, а не узел кассы. Общая строка о недоступной службе
    // называет узел, и владелец читал «Узел кассы недоступен · Кабинет БФД»
    // про работающий узел: касса при этом пробивает чеки, а не отвечает
    // только кабинет. Поэтому здесь стоят слова самого кабинета, а код
    // остаётся своим — поддержка по нему отличает молчание от отказа.
    is CabinetProblem.Unreachable -> Message.Refusal(texts.refusal.unreachable, UNREACHABLE)
    CabinetProblem.Unreadable -> Message.Refusal(texts.refusal.unreadable, UNREADABLE)
    CabinetProblem.NoNcaLayer -> Message.Refusal(texts.refusal.noNcaLayer, NCALAYER)
    is CabinetProblem.SignDeclined -> Message.Refusal(signWords(problem.detail, texts), SIGN)
    CabinetProblem.SessionExpired -> Message.Refusal(texts.refusal.sessionExpired, EXPIRED)
}

/**
 * Почему подпись не получена.
 *
 * Своё объяснение приложение даёт кодом — его и переводим. Всё прочее
 * пришло от NCALayer или посредника eGov mobile, и доходит как есть: это сообщение для поддержки,
 * и подменять его выдумкой хуже, чем показать чужими словами.
 *
 * Молчание NCALayer называется молчанием. Прежде оно приходило сюда
 * недоступностью, и владелец, подписавший в окне NCALayer, читал спустя
 * три минуты «Запустите его» про работающий NCALayer.
 */
private fun signWords(detail: String, texts: CabinetTexts): String {
    val reason = when {
        detail == Signer.WINDOW_CLOSED -> texts.refusal.signWindowClosed
        detail == Signer.NO_ANSWER -> texts.hints.signNoAnswer
        detail == Signer.CANCELLED -> texts.eds.cancelled
        detail == Signer.EGOV_UNREACHABLE -> texts.eds.egovUnreachable
        detail == Signer.EGOV_EXPIRED -> texts.eds.egovExpired
        // Отказ владельца NCALayer называет по-своему — «500 - action.canceled».
        // Это ответ службы, а не объяснение: подписывать отказался сам
        // владелец, и сказать об этом нужно его словами.
        cancelledBySigner(detail) -> texts.refusal.signCancelled
        else -> detail
    }
    return listOf(texts.refusal.signDeclined, reason).filter { it.isNotBlank() }.joinToString(Glyphs.SEPARATOR)
}

/** Отказ кабинета словами владельца; `null` — такого кода приложение не знает. */
private fun refusalWords(code: String, texts: CabinetTexts): String? = when (code) {
    "CASH_REGISTER_STATUS" -> texts.register.tokenOnlyRegistered
    // Самый частый отказ по снятию с учёта, и приложение умеет его
    // исправить само — кнопкой закрытия смены. Под кнопкой при этом
    // стояло английское «Shift is open» от кабинета.
    "SHIFT_IS_OPEN" -> texts.applications.shiftOpenTitle
    "STATE_UNKNOWN" -> texts.hints.technicalUnknown
    "KKM_NOT_ACTIVE" -> texts.refusal.kkmNotActive
    "DEFAULT_PIN_NOT_ALLOWED" -> texts.refusal.defaultPinNotAllowed
    "ACCESS_DENIED" -> texts.refusal.accessDenied
    else -> null
}

/** Коды помех самого приложения: у них нет ответа сервера, а показать код надо. */
private const val NCALAYER = "NCALAYER"
private const val SIGN = "SIGN"
private const val EXPIRED = "SESSION_EXPIRED"
private const val UNREACHABLE = "CABINET_UNREACHABLE"
private const val UNREADABLE = "CABINET_UNREADABLE"
