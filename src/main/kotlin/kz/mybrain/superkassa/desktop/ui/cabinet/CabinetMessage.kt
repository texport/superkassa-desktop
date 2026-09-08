package kz.mybrain.superkassa.desktop.ui.cabinet

import kz.mybrain.superkassa.desktop.app.CabinetProblem
import kz.mybrain.superkassa.desktop.app.Message
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts

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
fun cabinetMessage(problem: CabinetProblem, texts: CabinetTexts): Message = when (problem) {
    is CabinetProblem.Refused -> Message.Refusal(refusalWords(problem.code, texts) ?: problem.text, problem.code)
    is CabinetProblem.Unreachable -> Message.NodeUnavailable(texts.title, problem.reason)
    CabinetProblem.NoNcaLayer -> Message.Refusal(texts.noNcaLayer, NCALAYER)
    is CabinetProblem.SignDeclined -> Message.Refusal(
        listOf(texts.signDeclined, problem.detail).filter { it.isNotBlank() }.joinToString(" · "),
        SIGN
    )
    CabinetProblem.SessionExpired -> Message.Refusal(texts.sessionExpired, EXPIRED)
}

/** Отказ кабинета словами владельца; `null` — такого кода приложение не знает. */
private fun refusalWords(code: String, texts: CabinetTexts): String? = when (code) {
    "CASH_REGISTER_STATUS" -> texts.tokenOnlyRegistered
    "STATE_UNKNOWN" -> texts.technicalUnknownHint
    "KKM_NOT_ACTIVE" -> texts.kkmNotActive
    "DEFAULT_PIN_NOT_ALLOWED" -> texts.defaultPinNotAllowed
    else -> null
}

/** Коды помех самого приложения: у них нет ответа сервера, а показать код надо. */
private const val NCALAYER = "NCALAYER"
private const val SIGN = "SIGN"
private const val EXPIRED = "SESSION_EXPIRED"
