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
    // Молчит кабинет, а не узел кассы. Общая строка о недоступной службе
    // называет узел, и владелец читал «Узел кассы недоступен · Кабинет БФД»
    // про работающий узел: касса при этом пробивает чеки, а не отвечает
    // только кабинет. Поэтому здесь стоят слова самого кабинета, а код
    // остаётся своим — поддержка по нему отличает молчание от отказа.
    is CabinetProblem.Unreachable -> Message.Refusal(texts.unreachable, UNREACHABLE)
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
 *
 * Молчание NCALayer называется молчанием. Прежде оно приходило сюда
 * недоступностью, и владелец, подписавший в окне NCALayer, читал спустя
 * три минуты «Запустите его» про работающий NCALayer.
 */
private fun signWords(detail: String, texts: CabinetTexts): String {
    val reason = when (detail) {
        NcaLayer.WINDOW_CLOSED -> texts.signWindowClosed
        NcaLayer.NO_ANSWER -> texts.hints.signNoAnswer
        else -> detail
    }
    return listOf(texts.signDeclined, reason).filter { it.isNotBlank() }.joinToString(Glyphs.SEPARATOR)
}

/** Отказ кабинета словами владельца; `null` — такого кода приложение не знает. */
private fun refusalWords(code: String, texts: CabinetTexts): String? = when (code) {
    "CASH_REGISTER_STATUS" -> texts.tokenOnlyRegistered
    // Самый частый отказ по снятию с учёта, и приложение умеет его
    // исправить само — кнопкой закрытия смены. Под кнопкой при этом
    // стояло английское «Shift is open» от кабинета.
    "SHIFT_IS_OPEN" -> texts.shiftOpenTitle
    "STATE_UNKNOWN" -> texts.hints.technicalUnknown
    "KKM_NOT_ACTIVE" -> texts.kkmNotActive
    "DEFAULT_PIN_NOT_ALLOWED" -> texts.defaultPinNotAllowed
    else -> null
}

/** Коды помех самого приложения: у них нет ответа сервера, а показать код надо. */
private const val NCALAYER = "NCALAYER"
private const val SIGN = "SIGN"
private const val EXPIRED = "SESSION_EXPIRED"
private const val UNREACHABLE = "CABINET_UNREACHABLE"
