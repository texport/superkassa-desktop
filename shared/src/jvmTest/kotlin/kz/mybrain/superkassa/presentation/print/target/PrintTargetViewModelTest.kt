package kz.mybrain.superkassa.presentation.print.target

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.print.model.PrintKind
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.presentation.settings.FakePrintOut
import kz.mybrain.superkassa.presentation.settings.MemoryPrintChoices
import kz.mybrain.superkassa.presentation.settings.settingsPorts
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Принтер кассы: выбор держится за кассой, копии и вид файла — за машиной.
 *
 * За одним компьютером бывает две кассы, и чековая лента у каждой своя.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrintTargetViewModelTest {

    private val signIn = SignIn()
    private val printing = MemoryPrintChoices()
    private val out = FakePrintOut()
    private val app =
        CoreScene.app(FakeCore(), signIn, settings = settingsPorts().copy(printChoices = printing, printOut = out))

    @BeforeTest
    fun main() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun reset() = Dispatchers.resetMain()

    @Test
    fun `принтер выбирается за кассой, копии и вид — за машиной`() {
        signIn.enter(CoreScene.kkm(id = "kkm-1"), CoreScene.cashier(), CoreScene.PIN)
        val model = printTargetModel(app)

        model.choosePrinter("Чековый у кассы")
        model.chooseCopies(2)
        model.chooseKind(PrintKind.Html)

        assertEquals(listOf("Чековый у кассы"), model.state.value.printers)
        assertEquals("Чековый у кассы", printing.printer("kkm-1"))
        assertEquals(2, printing.copies)
        assertEquals(PrintKind.Html, printing.kind())
    }

    /** Принтеров на машине нет: карточка говорит об этом, а не предлагает системный молча. */
    @Test
    fun `машина без принтеров так и называется`() {
        out.names = emptyList()
        signIn.enter(CoreScene.kkm(id = "kkm-1"), CoreScene.cashier(), CoreScene.PIN)

        assertTrue(printTargetModel(app).state.value.noPrinters)
    }

    /** Выбранный принтер отключили: карточка называет это там, где выбирают другой. */
    @Test
    fun `отключённый принтер кассы назван на карточке`() {
        printing.choosePrinter("kkm-1", "Чековый у кассы")
        out.names = listOf("Конторский A4")
        signIn.enter(CoreScene.kkm(id = "kkm-1"), CoreScene.cashier(), CoreScene.PIN)

        assertTrue(printTargetModel(app).state.value.printerGone)
    }

    /** Другая касса — свой принтер: выбор прежней на неё не переносится. */
    @Test
    fun `смена кассы показывает её принтер`() {
        signIn.enter(CoreScene.kkm(id = "kkm-1"), CoreScene.cashier(), CoreScene.PIN)
        val model = printTargetModel(app)
        model.choosePrinter("Чековый у кассы")

        signIn.enter(CoreScene.kkm(id = "kkm-2"), CoreScene.cashier(), CoreScene.PIN)

        assertEquals("kkm-2", model.state.value.kkmId)
        assertNull(model.state.value.printer)
    }
}
