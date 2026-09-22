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
 * кабинет — из учёта КГД, БФД — из своего снимка. Сверки между ними
 * не было ни в одном экране, и расхождения выяснялись случайно: касса,
 * снятая с учёта, встречала кассира надписью «Активна».
 *
 * Здесь показания сводятся к одному ответу на каждый из двух вопросов —
 * в работе ли касса и открыта ли смена, — и расхождение становится видно
 * сразу. Сама сверка ничего не меняет: она показывает, кто с кем
 * не согласен, а исправляет расхождение владелец — перечитав состояние
 * или сняв кассу с учёта. Словами показания называет `StateWords`.
 */

/** Кто говорит о кассе. */
enum class StateSource(val title: (CabinetTexts) -> String) {

    /** Узел на этой машине: его база и есть то, чем работает кассир. */
    Node({ it.sourceNode }),

    /** Кабинет: учёт КГД — стоит ли касса на учёте. */
    Cabinet({ it.sourceCabinet }),

    /** БФД: база фискальных данных — работает ли касса и открыта ли смена. */
    Bfd({ it.sourceBfd })
}

/** Общий ответ на вопрос, который каждый источник понимает по-своему. */
enum class Verdict { Yes, No, Unknown }

/**
 * Что говорит один источник.
 *
 * @param record что записано в учёте КГД; заполняет его только кабинет.
 *   У него не «да и нет», а пять состояний, и все, кроме учтённого,
 *   сводились к одному «касса снята с учёта» — включая черновик,
 *   который на учёт никто не подавал.
 */
data class StateClaim(
    val source: StateSource,
    val usable: Verdict,
    val shift: Verdict,
    val record: KkmRecord? = null
)

/** Вопрос карточки и то поле показания, которым источник на него отвечает. */
enum class StateQuestion(val verdictOf: (StateClaim) -> Verdict) {
    Usable(StateClaim::usable),
    Shift(StateClaim::shift)
}

/**
 * Ответ по существу — то, что владелец читает первым.
 *
 * Отказ в работе бывает двух разных родов, и владельцу они говорят
 * разное: заблокированную кассу разблокируют на месте, снятую с учёта
 * возвращают заявлением в КГД. Одним словом «Нет» они не различались,
 * и владелец не понимал, что именно ему делать.
 */
enum class Headline { Working, Blocked, OffRecord, WorkUnknown, ShiftOpen, ShiftClosed, ShiftUnknown }

/**
 * Сведённый ответ на один вопрос.
 *
 * Показания лежат здесь все, вместе с молчащими: молчание тоже
 * показывается, иначе строка из одного источника рядом со строкой
 * из трёх читается как дефект разметки.
 */
data class StateAnswer(
    val question: StateQuestion,
    val headline: Headline,
    val claims: List<StateClaim>,
    val disagreeing: Set<StateSource>
)

/**
 * Сводит показания трёх источников.
 *
 * Чего источник не знает, остаётся [Verdict.Unknown] — незнание не то же,
 * что отрицание, и красить его расхождением нельзя.
 *
 * @param kkm касса на узле; `null` — на этой машине её нет.
 * @param register запись кабинета: учёт КГД.
 * @param technical снимок БФД; `null` — БФД о ней не спрашивали.
 * @param shift состояние смены по узлу.
 */
fun stateClaims(
    kkm: Kkm?,
    register: CabinetRegister,
    technical: TechnicalState?,
    shift: ShiftState
): List<StateClaim> = listOf(nodeClaim(kkm, shift), cabinetClaim(register), bfdClaim(technical))

/** Оба вопроса карточки, сведённые к одному ответу каждый. */
fun stateAnswers(claims: List<StateClaim>): List<StateAnswer> = StateQuestion.entries.map { question ->
    StateAnswer(
        question = question,
        headline = headline(question, claims.filter { question.verdictOf(it) != Verdict.Unknown }),
        claims = claims,
        disagreeing = disagreeingBy(claims, question.verdictOf)
    )
}

/**
 * Источники, чьи показания расходятся с остальными.
 *
 * Сравниваются только те, кто ответил: источник, который о кассе
 * не знает, ни с кем не спорит.
 */
fun disagreeing(claims: List<StateClaim>): Set<StateSource> =
    StateQuestion.entries.flatMap { disagreeingBy(claims, it.verdictOf) }.toSet()

private fun disagreeingBy(claims: List<StateClaim>, of: (StateClaim) -> Verdict): Set<StateSource> {
    val known = claims.filter { of(it) != Verdict.Unknown }
    if (known.map(of).distinct().size < 2) return emptySet()
    return known.map { it.source }.toSet()
}

/**
 * Ответ по существу.
 *
 * Про смену говорит тот, кто высказался первым, а первым в списке стоит
 * узел не случайно: его база и есть то, чем работает кассир, а БФД знает
 * лишь то, что до него доехало.
 */
private fun headline(question: StateQuestion, spoken: List<StateClaim>): Headline = when {
    spoken.isEmpty() && question == StateQuestion.Shift -> Headline.ShiftUnknown
    spoken.isEmpty() -> Headline.WorkUnknown
    question != StateQuestion.Shift -> workHeadline(spoken)
    spoken.first().shift == Verdict.Yes -> Headline.ShiftOpen
    else -> Headline.ShiftClosed
}

/** Снятая с учёта важнее заблокированной: это вопрос к КГД, а не к машине. */
private fun workHeadline(spoken: List<StateClaim>): Headline = when {
    spoken.any { it.source == StateSource.Cabinet && it.usable == Verdict.No } -> Headline.OffRecord
    spoken.any { it.usable == Verdict.No } -> Headline.Blocked
    else -> Headline.Working
}

/** Узел: заблокированная касса фискальных команд не выполняет. */
private fun nodeClaim(kkm: Kkm?, shift: ShiftState): StateClaim = StateClaim(
    source = StateSource.Node,
    usable = when (kkm?.state) {
        null -> Verdict.Unknown
        ACTIVE -> Verdict.Yes
        else -> Verdict.No
    },
    shift = when {
        kkm == null -> Verdict.Unknown
        shift == ShiftState.Open -> Verdict.Yes
        shift == ShiftState.Closed -> Verdict.No
        else -> Verdict.Unknown
    }
)

/**
 * Кабинет: на учёте стоит только зарегистрированная касса, а смену
 * он не ведёт вовсе — у него учёт КГД, а не работа машины.
 *
 * Пустой статус — незнание, а не отказ. Прежде на его месте стояла
 * ветка `null`, которой быть не может: поле обязательное, — и пустая
 * строка уходила под `else`, то есть читалась как «снята с учёта».
 */
private fun cabinetClaim(register: CabinetRegister): StateClaim = StateClaim(
    source = StateSource.Cabinet,
    usable = when {
        register.status.isBlank() -> Verdict.Unknown
        onRecord(register.status) -> Verdict.Yes
        else -> Verdict.No
    },
    shift = Verdict.Unknown,
    record = register.status.takeIf { it.isNotBlank() }?.let(::kkmRecord)
)

/** БФД: свой снимок кассы, которой он может и не знать вовсе. */
private fun bfdClaim(technical: TechnicalState?): StateClaim = StateClaim(
    source = StateSource.Bfd,
    usable = when {
        technical?.found != true || technical.active == null -> Verdict.Unknown
        technical.active == true -> Verdict.Yes
        else -> Verdict.No
    },
    shift = when (technical?.shiftStatus) {
        null -> Verdict.Unknown
        SHIFT_OPEN -> Verdict.Yes
        else -> Verdict.No
    }
)

/** Состояние узла, при котором касса работает. */
private const val ACTIVE = "ACTIVE"

/** Состояние смены в снимке БФД, означающее открытую смену. */
private const val SHIFT_OPEN = "OPEN"
