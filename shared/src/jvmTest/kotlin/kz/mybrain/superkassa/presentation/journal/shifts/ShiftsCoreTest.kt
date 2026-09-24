package kz.mybrain.superkassa.presentation.journal.shifts

import io.github.texport.superkassa.core.presentation.api.model.shift.ShiftStatus
import io.github.texport.superkassa.testing.api.kassa.ReadyKassa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.domain.document.model.printable
import kz.mybrain.superkassa.domain.journal.model.zReportId
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.kassa.appBench
import kz.mybrain.superkassa.kassa.appKassa
import kz.mybrain.superkassa.presentation.common.message.Notices
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Прошлые смены и документы смены на настоящем ядре и тестовом БФД:
 * смены закрыты и открыты кассиром, журнал читает их как есть.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShiftsCoreTest {
    private val directory: File = createTempDirectory("kassa-shifts-").toFile()
    private val bench = appBench(directory)
    private val kassa: ReadyKassa = bench.registerKassa(appKassa(adminPin = ADMIN, cashierPin = CASHIER))
    private val signIn = SignIn()

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun close() {
        Dispatchers.resetMain()
        bench.close()
        directory.deleteRecursively()
    }

    private fun model(): ShiftsViewModel {
        signIn.enter(kassa.info(), bench.api.authenticate(kassa.kkmId, CASHIER), CASHIER)
        return shiftsModel(CoreScene.app(EmbeddedKassa(bench.api, Dispatchers.Unconfined), signIn, Notices()))
            .also { it.reload() }
    }

    @Test
    fun `закрытая смена — с Z-отчётом и своими документами, открытая — без отчёта`() {
        kassa.openShift()
        kassa.sell()
        kassa.cashIn()
        kassa.closeShift()
        kassa.openShift()
        val model = model()

        val (open, closed) = model.state.value.shifts.also { assertEquals(2, it.size) }
        assertEquals(ShiftStatus.OPEN, open.status, "первой стоит не текущая смена")
        assertNull(open.zReportId, "у открытой смены предложен Z-отчёт")
        val report = assertNotNull(closed.zReportId, "у закрытой смены нет Z-отчёта")

        model.open(closed)

        val documents = model.state.value.documents
        assertTrue(documents.map { it.docType }.containsAll(listOf("SHIFT_OPEN", "SALE", "CASH_IN")), "$documents")
        assertTrue(documents.all { it.shiftNo == closed.shiftNo }, "в смене документы чужой смены")
        assertTrue(documents.any { it.id == report }, "Z-отчёта нет среди документов смены")
        assertTrue(documents.all { it.printable })
    }

    @Test
    fun `смена дольше суток видна открытой — журнал её не прячет`() {
        kassa.openShift()
        kassa.sell()
        kassa.clock.move(DAY_AND_HOUR)

        val shift = model().state.value.shifts.single()

        assertEquals(ShiftStatus.OPEN, shift.status)
    }

    private companion object {
        const val ADMIN = "7391"
        const val CASHIER = "4826"
        const val DAY_AND_HOUR = 25L * 60 * 60 * 1000
    }
}
