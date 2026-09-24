package kz.mybrain.superkassa.presentation.cabinet.register.component

import kz.mybrain.superkassa.presentation.cabinet.register.component.StateScene.answer
import kz.mybrain.superkassa.presentation.cabinet.register.component.StateScene.bfd
import kz.mybrain.superkassa.presentation.cabinet.register.component.StateScene.cabinetRegister
import kz.mybrain.superkassa.presentation.cabinet.register.component.StateScene.node
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
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

    private val texts = textsOf(Language.Ru).cabinet

    @Test
    fun `все согласны — расхождений нет`() {
        val claims = stateClaims(
            kkm = node("ACTIVE"),
            register = cabinetRegister("REGISTERED"),
            technical = bfd(active = true, shift = "CLOSED")
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
    fun `снятая с учёта касса при активной кассе машины — расхождение`() {
        val claims = stateClaims(
            kkm = node("ACTIVE"),
            register = cabinetRegister("DEREGISTERED"),
            technical = null
        )

        assertEquals(setOf(StateSource.Node, StateSource.Cabinet), disagreeing(claims))
    }

    /**
     * Снятая с учёта и заблокированная — разные ответы: первую возвращают
     * заявлением в КГД, вторую разблокируют на месте.
     */
    @Test
    fun `учёт и блокировка названы разными ответами`() {
        val offRecord = stateClaims(node("ACTIVE"), cabinetRegister("DEREGISTERED"), null)
        val blocked = stateClaims(node("BLOCKED"), cabinetRegister("REGISTERED"), null)

        assertEquals(Headline.OffRecord, answer(offRecord, StateQuestion.Usable).headline)
        assertEquals(Headline.Blocked, answer(blocked, StateQuestion.Usable).headline)
        assertFalse(texts.headlineWords(Headline.OffRecord) == texts.headlineWords(Headline.Blocked))
    }

    /** Касса машины считает смену закрытой, ОФД держит её открытой. */
    @Test
    fun `расхождение по смене видно отдельно от состояния`() {
        val claims = stateClaims(
            kkm = node("ACTIVE"),
            register = cabinetRegister("REGISTERED"),
            technical = bfd(active = true, shift = "OPEN")
        )

        assertEquals(setOf(StateSource.Node, StateSource.Bfd), disagreeing(claims))
        assertTrue(answer(claims, StateQuestion.Usable).disagreeing.isEmpty())
        assertEquals(setOf(StateSource.Node, StateSource.Bfd), answer(claims, StateQuestion.Shift).disagreeing)
    }

    /**
     * Незнание — не отрицание. Касса, заведённая на другой машине,
     * кассе этой машины неизвестна, и спорить ей не с кем.
     */
    @Test
    fun `незнание источника расхождением не считается`() {
        val claims = stateClaims(
            kkm = null,
            register = cabinetRegister("REGISTERED"),
            technical = bfd(active = null, shift = null)
        )

        assertTrue(disagreeing(claims).isEmpty())
        assertEquals(Verdict.Unknown, claims.first { it.source == StateSource.Node }.usable)
    }

    /** Кабинет о смене не знает: у него учёт, а не работа кассы. */
    @Test
    fun `кабинет о смене не высказывается`() {
        val claims = stateClaims(
            kkm = node("ACTIVE", shiftOpen = true),
            register = cabinetRegister("REGISTERED"),
            technical = bfd(active = true, shift = "OPEN")
        )

        assertEquals(Verdict.Unknown, claims.first { it.source == StateSource.Cabinet }.shift)
        assertTrue(disagreeing(claims).isEmpty())
    }
}
