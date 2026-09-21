package kz.mybrain.superkassa.desktop.ui.cabinet

import kz.mybrain.superkassa.desktop.server.cabinet.TechnicalState
import kz.mybrain.superkassa.desktop.ui.components.StatusTone
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs

/**
 * Показания источников словами владельца.
 *
 * Плашка складывалась из имени источника и общего «Да»/«Нет», и владелец
 * читал «Смена · ОФД · Нет» как отказ ОФД, а не как закрытую смену: чтобы
 * понять надпись, нужно было знать, какой вопрос задан строкой выше и что
 * на него отвечает каждый источник. Теперь плашка — законченное
 * утверждение: кто говорит и что именно, — и читается отдельно от всего.
 *
 * Слова собраны здесь, а не в разметке: разметка ставит их на место,
 * а что в них написано — предметный вопрос, и меняется он отдельно.
 */

/** Ответ по существу — крупной строкой над плашками. */
fun CabinetTexts.headlineWords(headline: Headline): String = when (headline) {
    Headline.Working -> stateWorking
    Headline.Blocked -> stateBlocked
    Headline.OffRecord -> stateOffRecord
    Headline.WorkUnknown -> stateWorkUnknown
    Headline.ShiftOpen -> shiftOpen
    Headline.ShiftClosed -> shiftClosed
    Headline.ShiftUnknown -> stateShiftUnknown
}

/**
 * Роль цвета ответа.
 *
 * Отказ красит то, с чем владельцу нужно что-то сделать; незнание —
 * ожидание, а не отказ; закрытая смена — тоже ожидание: это обычное
 * состояние кассы до начала дня.
 */
fun headlineTone(headline: Headline): StatusTone = when (headline) {
    Headline.Working, Headline.ShiftOpen -> StatusTone.Good
    Headline.Blocked, Headline.OffRecord -> StatusTone.Bad
    else -> StatusTone.Waiting
}

/** Плашка одного источника: кто говорит и что именно. */
fun CabinetTexts.claimWords(
    claim: StateClaim,
    question: StateQuestion,
    technical: TechnicalState?
): String {
    val said = when (question) {
        StateQuestion.Usable -> usableWords(claim, technical)
        StateQuestion.Shift -> shiftWords(claim, technical)
    }
    return claim.source.title(this) + Glyphs.SEPARATOR + said
}

/**
 * Молчание БФД, объяснённое строкой под показаниями.
 *
 * Разница владельцу важна: если БФД не ответил, карточку перечитывают,
 * а если он кассу ещё не видел — ждут первого обращения самой кассы.
 */
fun CabinetTexts.bfdSilenceTitle(technical: TechnicalState?): String =
    if (technical == null) sourceBfd else technicalUnknown

fun CabinetTexts.bfdSilenceHint(technical: TechnicalState?): String =
    if (technical == null) bfdNoAnswerHint else technicalUnknownHint

/**
 * Работает ли касса.
 *
 * Кабинет отвечает о другом: у него учёт КГД, а не работа машины, —
 * и словами он говорит про учёт. Под общим «Да» эта разница пропадала,
 * и владелец не видел, что кассу нужно возвращать заявлением.
 */
private fun CabinetTexts.usableWords(claim: StateClaim, technical: TechnicalState?): String = when {
    claim.source == StateSource.Cabinet -> recordWords(claim.usable)
    claim.usable == Verdict.Yes -> claimWorking
    claim.usable == Verdict.No -> claimBlocked
    claim.source == StateSource.Node -> claimNodeNoKkm
    else -> bfdSilenceWords(technical)
}

private fun CabinetTexts.recordWords(verdict: Verdict): String = when (verdict) {
    Verdict.Yes -> claimOnRecord
    Verdict.No -> claimOffRecord
    Verdict.Unknown -> claimRecordUnread
}

/**
 * Открыта ли смена.
 *
 * Кабинет смену не ведёт вовсе, и это не «не знает сейчас»: спрашивать
 * его о смене незачем. Сказано так и есть — иначе владелец ждёт от него
 * ответа, которого не будет никогда.
 */
private fun CabinetTexts.shiftWords(claim: StateClaim, technical: TechnicalState?): String = when {
    claim.source == StateSource.Cabinet -> claimShiftNotKept
    claim.shift == Verdict.Yes -> claimShiftOpen
    claim.shift == Verdict.No -> claimShiftClosed
    claim.source == StateSource.Node -> claimNodeNoKkm
    else -> bfdSilenceWords(technical)
}

private fun CabinetTexts.bfdSilenceWords(technical: TechnicalState?): String =
    if (technical == null) claimBfdNoAnswer else claimBfdNoKkm
