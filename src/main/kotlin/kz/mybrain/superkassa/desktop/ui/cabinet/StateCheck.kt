package kz.mybrain.superkassa.desktop.ui.cabinet

import kz.mybrain.superkassa.desktop.app.ShiftState
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.TechnicalState
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts

/**
 * Сверка состояний кассы по всем, кто о ней знает.
 *
 * О кассе говорят трое, и каждый со своего места: узел — из своей базы,
 * кабинет — из учёта КГД, ОФД — из своего снимка. Сверки между ними
 * не было ни в одном экране, и расхождения выяснялись случайно: касса,
 * снятая с учёта, встречала кассира надписью «Активна», а в кабинете
 * состояние бывало устаревшим.
 *
 * Здесь они сводятся к одному ответу на два вопроса — в работе ли касса
 * и открыта ли смена, — и расхождение становится видно сразу. Сама сверка
 * ничего не меняет: она показывает, кто с кем не согласен, а исправляет
 * расхождение владелец — перечитав состояние или сняв кассу с учёта.
 */

/** Кто говорит о кассе. */
enum class StateSource(val title: (CabinetTexts) -> String) {

    /** Узел на этой машине: его база и есть то, чем работает кассир. */
    Node({ it.sourceNode }),

    /** Кабинет: учёт КГД — стоит ли касса на учёте. */
    Cabinet({ it.sourceCabinet }),

    /** ОФД: его снимок о кассе — работает ли она и открыта ли смена. */
    Ofd({ it.sourceOfd })
}

/** Общий ответ на вопрос, который каждый источник понимает по-своему. */
enum class Verdict { Yes, No, Unknown }

/** Что говорит один источник. */
data class StateClaim(val source: StateSource, val usable: Verdict, val shift: Verdict)

/**
 * Сводит показания трёх источников.
 *
 * Чего источник не знает, остаётся [Verdict.Unknown] — незнание не то же,
 * что отрицание, и красить его расхождением нельзя.
 *
 * @param kkm касса на узле; `null` — на этой машине её нет.
 * @param register запись кабинета: учёт КГД.
 * @param technical снимок ОФД; `null` — ОФД о ней не спрашивали.
 * @param shift состояние смены по узлу.
 */
fun stateClaims(
    kkm: Kkm?,
    register: CabinetRegister,
    technical: TechnicalState?,
    shift: ShiftState
): List<StateClaim> = listOf(
    StateClaim(StateSource.Node, nodeUsable(kkm), nodeShift(kkm, shift)),
    StateClaim(StateSource.Cabinet, cabinetUsable(register), Verdict.Unknown),
    StateClaim(StateSource.Ofd, ofdUsable(technical), ofdShift(technical))
)

/**
 * Источники, чьи показания расходятся с остальными.
 *
 * Сравниваются только те, кто ответил: источник, который о кассе
 * не знает, ни с кем не спорит.
 */
fun disagreeing(claims: List<StateClaim>): Set<StateSource> =
    disagreeingBy(claims) { it.usable } + disagreeingBy(claims) { it.shift }

private fun disagreeingBy(claims: List<StateClaim>, of: (StateClaim) -> Verdict): Set<StateSource> {
    val known = claims.filter { of(it) != Verdict.Unknown }
    if (known.map(of).distinct().size < 2) return emptySet()
    return known.map { it.source }.toSet()
}

/** Узел: заблокированная касса фискальных команд не выполняет. */
private fun nodeUsable(kkm: Kkm?): Verdict = when (kkm?.state) {
    null -> Verdict.Unknown
    ACTIVE -> Verdict.Yes
    else -> Verdict.No
}

private fun nodeShift(kkm: Kkm?, shift: ShiftState): Verdict = when {
    kkm == null -> Verdict.Unknown
    shift == ShiftState.Open -> Verdict.Yes
    shift == ShiftState.Closed -> Verdict.No
    else -> Verdict.Unknown
}

/** Кабинет: на учёте стоит только зарегистрированная касса. */
private fun cabinetUsable(register: CabinetRegister): Verdict = when (register.status) {
    null -> Verdict.Unknown
    in ON_RECORD -> Verdict.Yes
    else -> Verdict.No
}

private fun ofdUsable(technical: TechnicalState?): Verdict = when {
    technical == null || !technical.found -> Verdict.Unknown
    technical.active == null -> Verdict.Unknown
    technical.active == true -> Verdict.Yes
    else -> Verdict.No
}

private fun ofdShift(technical: TechnicalState?): Verdict = when (technical?.shiftStatus) {
    null -> Verdict.Unknown
    SHIFT_OPEN -> Verdict.Yes
    else -> Verdict.No
}

/** Состояние узла, при котором касса работает. */
private const val ACTIVE = "ACTIVE"

/** Состояние смены в снимке ОФД, означающее открытую смену. */
private const val SHIFT_OPEN = "OPEN"

/** Состояния кабинета, при которых касса стоит на учёте. */
private val ON_RECORD = setOf("REGISTERED", "REGISTERED_REREGISTRATION_SUCCESS")
