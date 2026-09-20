package kz.mybrain.superkassa.desktop.ui.cabinet

import kz.mybrain.superkassa.desktop.app.CabinetProblem
import kz.mybrain.superkassa.desktop.app.Message
import kz.mybrain.superkassa.desktop.eds.NcaLayer
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs

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
    is CabinetProblem.Unreachable -> Message.NodeUnavailable(texts.title)
    CabinetProblem.NoNcaLayer -> Message.Refusal(texts.noNcaLayer, NCALAYER)
    is CabinetProblem.SignDeclined -> Message.Refusal(signWords(problem.detail, texts), SIGN)
    CabinetProblem.SessionExpired -> Message.Refusal(texts.sessionExpired, EXPIRED)
}

/**
 * Почему подпись не получена.
 *
 * Своё объяснение приложение даёт кодом — его и переводим. Всё прочее
 * пришло от NCALayer, и доходит как есть: это сообщение для поддержки,
 * и подменять его выдумкой хуже, чем показать чужими словами.
 */
private fun signWords(detail: String, texts: CabinetTexts): String {
    val reason = if (detail == NcaLayer.WINDOW_CLOSED) texts.signWindowClosed else detail
    return listOf(texts.signDeclined, reason).filter { it.isNotBlank() }.joinToString(Glyphs.SEPARATOR)
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
