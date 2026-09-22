package kz.mybrain.superkassa.desktop.ui.strings

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
)

/**
 * Причина блокировки по её коду.
 *
 * `null` кода или незнакомый код — общая причина: «касса заблокирована»
 * без домыслов о том, чего приложение не знает.
 */
fun blockReasonWords(code: Int?, language: Language): String {
    val texts = blockReasonTexts(language)
    return when (code) {
        INVALID_TOKEN -> texts.invalidToken
        SHIFT_TOO_LONG -> texts.shiftTooLong
        AUTONOMOUS_TOO_LONG -> texts.autonomousTooLong
        DEREGISTERED -> texts.deregistered
        DISCONNECTED -> texts.disconnected
        INCORRECT_DATA -> texts.incorrectData
        else -> texts.unknown
    }
}

fun blockReasonTexts(language: Language): BlockReasonTexts = when (language) {
    Language.Kk -> blockReasonKk
    Language.Ru -> blockReasonRu
    Language.En -> blockReasonEn
}

/** Отказ БФД, перенесённый узлом в свой диапазон: код протокола плюс тысяча. */
private const val INCORRECT_DATA = 1013
private const val INVALID_TOKEN = 1002
private const val DEREGISTERED = 1018
private const val DISCONNECTED = 1019

/** Свои причины узла. */
private const val SHIFT_TOO_LONG = 1011
private const val AUTONOMOUS_TOO_LONG = 2001

private val blockReasonRu = BlockReasonTexts(
    unknown = "Касса заблокирована",
    invalidToken = "Токен кассы недействителен: получите новый в кабинете и впишите его в настройках",
    shiftTooLong = "Смена открыта дольше суток: закройте её Z-отчётом",
    autonomousTooLong = "Автономная работа шла дольше допустимого: восстановите связь с БФД",
    deregistered = "Касса снята с учёта",
    disconnected = "Касса отключена от БФД",
    incorrectData = "БФД отверг данные кассы",
    readingStays = "Заблокированной кассе остаётся чтение: журнал, смены и отчёты."
)

private val blockReasonKk = BlockReasonTexts(
    unknown = "Касса бұғатталған",
    invalidToken = "Касса токені жарамсыз: кабинеттен жаңасын алып, баптауларға жазыңыз",
    shiftTooLong = "Ауысым тәуліктен ұзақ ашық: оны Z-есеппен жабыңыз",
    autonomousTooLong = "Автономды жұмыс рұқсат етілгеннен ұзақ жүрді: БФД байланысын қалпына келтіріңіз",
    deregistered = "Касса есептен шығарылды",
    disconnected = "Касса БФД-дан ажыратылған",
    incorrectData = "БФД касса деректерін қабылдамады",
    readingStays = "Бұғатталған кассаға оқу қалады: журнал, ауысымдар және есептер."
)

private val blockReasonEn = BlockReasonTexts(
    unknown = "The register is blocked",
    invalidToken = "The register token is invalid: issue a new one in the cabinet and enter it in the settings",
    shiftTooLong = "The shift has been open for over a day: close it with a Z-report",
    autonomousTooLong = "Autonomous work lasted too long: restore the connection to the BFD",
    deregistered = "The register is deregistered",
    disconnected = "The register is disconnected from the BFD",
    incorrectData = "The BFD rejected the register data",
    readingStays = "A blocked register stays readable: journal, shifts and reports."
)
