package kz.mybrain.superkassa.presentation.shift.dashboard

import kz.kazakhtelecom.proto.v203.OperationTypeEnum
import kz.mybrain.superkassa.domain.shift.model.ShiftState
import kz.mybrain.superkassa.kassa.CoreDesk
import kz.mybrain.superkassa.kassa.tiyn
import kz.mybrain.superkassa.presentation.common.message.Message
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Главный экран на настоящем ядре: смена, X- и Z-отчёт, наличные.
 *
 * Итог сверяется с ядром — смена, документы, остаток — и с тем, что
 * получил тестовый БФД: отчёт с теми же суммами, что пробиты.
 */
class DashboardCoreTest {
    private val desk = CoreDesk()

    @AfterTest
    fun close() = desk.close()

    @Test
    fun `администратор открывает смену — смена открыта, в БФД ничего не ушло`() {
        val kassa = desk.register()
        desk.sit(kassa, admin = true)
        val model = dashboardModel(desk.services)
        assertEquals(ShiftState.Closed, model.state.value.shift)

        model.openShift()

        assertIs<Message.Done>(desk.said, desk.saidText)
        assertEquals(ShiftState.Open, model.state.value.shift)
        assertEquals(1L, model.state.value.shiftNumber)
        assertTrue(desk.bfd.requests.none { it.ticket != null || it.close_shift != null })
    }

    @Test
    fun `X-отчёт уходит в БФД с продажами смены и наличными ящика`() {
        val kassa = desk.seated()
        kassa.sell("500.00", "3")
        kassa.cashIn("2000.00")
        val model = dashboardModel(desk.services)

        model.xReport()

        assertIs<Message.Done>(desk.said, desk.saidText)
        val report = assertNotNull(desk.bfd.xReports().lastOrNull(), "X-отчёт не дошёл до БФД")
        val sells = report.ticket_operations.single { it.operation == OperationTypeEnum.OPERATION_SELL }
        assertEquals(1, sells.tickets_count)
        assertEquals(SOLD, sells.tickets_sum.tiyn())
        assertEquals(SOLD + DEPOSIT, report.cash_sum.tiyn())
        assertEquals(SOLD + DEPOSIT, model.state.value.cashInDrawer, "плитка ящика расходится с отчётом")
        val kinds = model.state.value.documents.map { it.docType }.sorted()
        assertEquals(listOf("CASH_IN", "SALE", "SHIFT_OPEN", "X_REPORT"), kinds)
    }

    @Test
    fun `Z-отчёт закрывает смену, в БФД — закрытие с суммами смены`() {
        val kassa = desk.register().also { it.openShift() }
        kassa.sell("500.00", "3")
        desk.sit(kassa, admin = true)
        val model = dashboardModel(desk.services)

        model.closeShift()

        assertIs<Message.Done>(desk.said, desk.saidText)
        assertEquals(ShiftState.Closed, model.state.value.shift)
        val z = desk.bfd.closeShifts().single().z_report
        val sells = z?.ticket_operations?.single { it.operation == OperationTypeEnum.OPERATION_SELL }
        assertEquals(SOLD, sells?.tickets_sum.tiyn())
    }

    @Test
    fun `кассир закрывает смену своим пином — кнопка Z-отчёта у него не ведёт к отказу`() {
        desk.seated()
        val model = dashboardModel(desk.services)

        model.closeShift()

        assertIs<Message.Done>(desk.said, "кассиру отказано в Z-отчёте: ${desk.saidText}")
        assertEquals(ShiftState.Closed, model.state.value.shift)
    }

    @Test
    fun `отклонённый БФД чек виден на главном экране с именем кассира`() {
        val kassa = desk.seated()
        kassa.rejectedSale()
        val model = dashboardModel(desk.services)

        val refused = model.state.value.refused.single()

        assertEquals("SALE", refused.docType)
        assertEquals("Нурлан", model.state.value.operators[refused.id])
    }

    @Test
    fun `сутки смены прошли — экран говорит о пределе по часам кассы`() {
        val kassa = desk.seated()
        kassa.sell()
        val model = dashboardModel(desk.services)
        assertNotNull(model.state.value.dayLimitAt, "предел не назван после первого чека")

        kassa.clock.move(DAY_AND_MINUTE)
        model.refresh()

        assertTrue(model.state.value.dayLimitExceeded, "сутки прошли, а экран молчит")
    }

    private companion object {
        const val SOLD = 150_000L
        const val DEPOSIT = 200_000L
        const val DAY_AND_MINUTE = 24L * 60 * 60 * 1000 + 60_000
    }
}
