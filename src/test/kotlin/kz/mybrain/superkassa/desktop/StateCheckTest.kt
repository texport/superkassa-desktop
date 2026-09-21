package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.app.ShiftState
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.TechnicalState
import kz.mybrain.superkassa.desktop.ui.cabinet.StateSource
import kz.mybrain.superkassa.desktop.ui.cabinet.Verdict
import kz.mybrain.superkassa.desktop.ui.cabinet.disagreeing
import kz.mybrain.superkassa.desktop.ui.cabinet.stateClaims
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Сверка состояний кассы.
 *
 * О кассе говорят трое, каждый со своего места, и сверки между ними
 * не было ни в одном экране: снятая с учёта касса встречала кассира
 * надписью «Активна», а в кабинете состояние бывало устаревшим.
 */
class StateCheckTest {

    private fun node(state: String) = Kkm(kkmId = "k-1", state = state, ofdSystemId = "5000021")

    private fun register(status: String) =
        CabinetRegister(id = "r-1", kkmId = 5_000_021, status = status)

    private fun ofd(active: Boolean?, shift: String?) =
        TechnicalState(found = true, active = active, shiftStatus = shift)

    @Test
    fun `все согласны — расхождений нет`() {
        val claims = stateClaims(
            kkm = node("ACTIVE"),
            register = register("REGISTERED"),
            technical = ofd(active = true, shift = "CLOSED"),
            shift = ShiftState.Closed
        )

        assertTrue(disagreeing(claims).isEmpty())
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

    /** Узел считает смену закрытой, ОФД держит её открытой. */
    @Test
    fun `расхождение по смене видно отдельно от состояния`() {
        val claims = stateClaims(
            kkm = node("ACTIVE"),
            register = register("REGISTERED"),
            technical = ofd(active = true, shift = "OPEN"),
            shift = ShiftState.Closed
        )

        assertEquals(setOf(StateSource.Node, StateSource.Ofd), disagreeing(claims))
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
            technical = ofd(active = null, shift = null),
            shift = ShiftState.Unknown
        )

        assertTrue(disagreeing(claims).isEmpty())
        assertEquals(Verdict.Unknown, claims.first { it.source == StateSource.Node }.usable)
    }

    /** Кабинет о смене не знает вовсе: у него учёт, а не работа кассы. */
    @Test
    fun `кабинет о смене не высказывается`() {
        val claims = stateClaims(
            kkm = node("ACTIVE"),
            register = register("REGISTERED"),
            technical = ofd(active = true, shift = "OPEN"),
            shift = ShiftState.Open
        )

        assertEquals(Verdict.Unknown, claims.first { it.source == StateSource.Cabinet }.shift)
        assertTrue(disagreeing(claims).isEmpty())
    }
}
