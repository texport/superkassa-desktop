package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.app.ShiftState
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.TechnicalState
import kz.mybrain.superkassa.desktop.ui.cabinet.Headline
import kz.mybrain.superkassa.desktop.ui.cabinet.StateAnswer
import kz.mybrain.superkassa.desktop.ui.cabinet.StateClaim
import kz.mybrain.superkassa.desktop.ui.cabinet.StateQuestion
import kz.mybrain.superkassa.desktop.ui.cabinet.StateSource
import kz.mybrain.superkassa.desktop.ui.cabinet.Verdict
import kz.mybrain.superkassa.desktop.ui.cabinet.claimWords
import kz.mybrain.superkassa.desktop.ui.cabinet.disagreeing
import kz.mybrain.superkassa.desktop.ui.cabinet.headlineWords
import kz.mybrain.superkassa.desktop.ui.cabinet.stateAnswers
import kz.mybrain.superkassa.desktop.ui.cabinet.stateClaims
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Сверка состояний кассы.
 *
 * О кассе говорят трое, каждый со своего места, и сверки между ними
 * не было ни в одном экране: снятая с учёта касса встречала кассира
 * надписью «Активна». Проверяется и то, что владелец читает: прежде
 * плашка складывалась из имени и общего «Нет», и «Смена · ОФД · Нет»
 * владелец понимал как отказ ОФД.
 */
class StateCheckTest {

    private val texts = cabinetTexts(Language.Ru)

    private fun node(state: String) = Kkm(kkmId = "k-1", state = state, ofdSystemId = "5000021")

    private fun register(status: String) =
        CabinetRegister(id = "r-1", kkmId = 5_000_021, status = status)

    private fun bfd(active: Boolean?, shift: String?) =
        TechnicalState(found = true, active = active, shiftStatus = shift)

    private fun answer(claims: List<StateClaim>, question: StateQuestion): StateAnswer =
        stateAnswers(claims).first { it.question == question }

    @Test
    fun `все согласны — расхождений нет`() {
        val claims = stateClaims(
            kkm = node("ACTIVE"),
            register = register("REGISTERED"),
            technical = bfd(active = true, shift = "CLOSED"),
            shift = ShiftState.Closed
        )

        assertTrue(disagreeing(claims).isEmpty())
        assertEquals(Headline.Working, answer(claims, StateQuestion.Usable).headline)
        assertEquals(Headline.ShiftClosed, answer(claims, StateQuestion.Shift).headline)
    }

    /**
     * Тот самый случай: касса снята с учёта в кабинете, а узел держит
     * её активной — и кассир открыл на ней смену.
     */
    @Test
    fun `снятая с учёта касса при активном узле — расхождение`() {
        val claims = stateClaims(
            kkm = node("ACTIVE"),
            register = register("DEREGISTERED"),
            technical = null,
            shift = ShiftState.Closed
        )

        assertEquals(setOf(StateSource.Node, StateSource.Cabinet), disagreeing(claims))
    }

    /**
     * Снятая с учёта и заблокированная — разные ответы: первую возвращают
     * заявлением в КГД, вторую разблокируют на месте.
     */
    @Test
    fun `учёт и блокировка названы разными ответами`() {
        val offRecord = stateClaims(node("ACTIVE"), register("DEREGISTERED"), null, ShiftState.Closed)
        val blocked = stateClaims(node("BLOCKED"), register("REGISTERED"), null, ShiftState.Closed)

        assertEquals(Headline.OffRecord, answer(offRecord, StateQuestion.Usable).headline)
        assertEquals(Headline.Blocked, answer(blocked, StateQuestion.Usable).headline)
        assertFalse(texts.headlineWords(Headline.OffRecord) == texts.headlineWords(Headline.Blocked))
    }

    /** Узел считает смену закрытой, ОФД держит её открытой. */
    @Test
    fun `расхождение по смене видно отдельно от состояния`() {
        val claims = stateClaims(
            kkm = node("ACTIVE"),
            register = register("REGISTERED"),
            technical = bfd(active = true, shift = "OPEN"),
            shift = ShiftState.Closed
        )

        assertEquals(setOf(StateSource.Node, StateSource.Bfd), disagreeing(claims))
        assertTrue(answer(claims, StateQuestion.Usable).disagreeing.isEmpty())
        assertEquals(setOf(StateSource.Node, StateSource.Bfd), answer(claims, StateQuestion.Shift).disagreeing)
    }

    /**
     * Незнание — не отрицание. Касса, заведённая на другой машине,
     * узлу неизвестна, и спорить ей не с кем.
     */
    @Test
    fun `незнание источника расхождением не считается`() {
        val claims = stateClaims(
            kkm = null,
            register = register("REGISTERED"),
            technical = bfd(active = null, shift = null),
            shift = ShiftState.Unknown
        )

        assertTrue(disagreeing(claims).isEmpty())
        assertEquals(Verdict.Unknown, claims.first { it.source == StateSource.Node }.usable)
    }

    /** Кабинет о смене не знает: у него учёт, а не работа кассы. */
    @Test
    fun `кабинет о смене не высказывается`() {
        val claims = stateClaims(
            kkm = node("ACTIVE"),
            register = register("REGISTERED"),
            technical = bfd(active = true, shift = "OPEN"),
            shift = ShiftState.Open
        )

        assertEquals(Verdict.Unknown, claims.first { it.source == StateSource.Cabinet }.shift)
        assertTrue(disagreeing(claims).isEmpty())
    }

    /**
     * О смене не высказался никто: касса заведена на другой машине,
     * а БФД её ещё не видел. Пустой строки на этом месте быть не должно.
     */
    @Test
    fun `о смене молчат все — ответ всё равно сказан словами`() {
        val claims = stateClaims(null, register("REGISTERED"), null, ShiftState.Unknown)
        val shift = answer(claims, StateQuestion.Shift)

        assertEquals(Headline.ShiftUnknown, shift.headline)
        assertEquals(3, shift.claims.size, "молчащие источники остаются на экране")
        assertTrue(texts.headlineWords(shift.headline).isNotBlank())
    }

    /**
     * Плашка читается отдельно от всего: в ней сказано, кто говорит
     * и что именно, а не «Нет» под подписью строкой выше.
     */
    @Test
    fun `плашка источника — законченное утверждение`() {
        val claims = stateClaims(
            kkm = node("ACTIVE"),
            register = register("DEREGISTERED"),
            technical = bfd(active = true, shift = "CLOSED"),
            shift = ShiftState.Closed
        )
        val snapshot = bfd(active = true, shift = "CLOSED")
        val words = claims.map { texts.claimWords(it, StateQuestion.Usable, snapshot) }

        assertEquals(
            listOf(
                "${texts.sourceNode}${Glyphs.SEPARATOR}${texts.claimWorking}",
                "${texts.sourceCabinet}${Glyphs.SEPARATOR}${texts.claimOffRecord}",
                "${texts.sourceBfd}${Glyphs.SEPARATOR}${texts.claimWorking}"
            ),
            words
        )
    }

    /**
     * Кабинет говорит о кассе то, что у него записано.
     *
     * У него не «да и нет», а учёт КГД с пятью состояниями, и все, кроме
     * учтённого, сводились к одному «касса снята с учёта». Так карточка
     * говорила о черновике, который владелец завёл час назад и никуда
     * ещё не подавал, — а черновиков у сети показа три тысячи из трёх
     * тысяч трёхсот. Снятие с учёта владелец затевает сам и знает о нём;
     * прочесть о нём там, где его не было, — повод бежать разбираться.
     */
    @Test
    fun `кабинет не объявляет снятым с учёта то, что на учёт не ставилось`() {
        val said = listOf("DRAFT", "REGISTRATION_IN_ISNA_PROCESS", "REGISTRATION_IN_ISNA_ERROR").map { status ->
            val claims = stateClaims(null, register(status), null, ShiftState.Unknown)
            val cabinet = claims.first { it.source == StateSource.Cabinet }
            status to texts.claimWords(cabinet, StateQuestion.Usable, null).substringAfter(Glyphs.SEPARATOR)
        }

        said.forEach { (status, words) ->
            assertFalse(words == texts.claimOffRecord, "о состоянии $status сказано «$words»")
        }
        assertEquals(said.map { it.second }.distinct().size, said.size, "три разных состояния названы одинаково")
        val deregistered = stateClaims(null, register("DEREGISTERED"), null, ShiftState.Unknown)
        assertEquals(
            texts.claimOffRecord,
            texts.claimWords(
                deregistered.first { it.source == StateSource.Cabinet },
                StateQuestion.Usable,
                null
            ).substringAfter(Glyphs.SEPARATOR),
            "снятая с учёта перестала называться снятой"
        )
    }

    /**
     * Молчание тоже сказано словами, и у каждого оно своё: БФД не ответил
     * вовсе — перечитать карточку; БФД кассу ещё не видел — ждать её
     * первого обращения; кабинет смену не ведёт по устройству, и ответа
     * от него не будет никогда.
     */
    @Test
    fun `молчание каждого источника объяснено своими словами`() {
        val bfd = StateClaim(StateSource.Bfd, Verdict.Unknown, Verdict.Unknown)
        val cabinet = StateClaim(StateSource.Cabinet, Verdict.Yes, Verdict.Unknown)

        assertEquals(
            listOf(texts.claimBfdNoAnswer, texts.claimBfdNoKkm, texts.claimShiftNotKept),
            listOf(
                texts.claimWords(bfd, StateQuestion.Usable, null),
                texts.claimWords(bfd, StateQuestion.Usable, TechnicalState()),
                texts.claimWords(cabinet, StateQuestion.Shift, null)
            ).map { it.substringAfter(Glyphs.SEPARATOR) }
        )
    }
}
