package kz.mybrain.superkassa.presentation.print.preview

import io.github.texport.superkassa.core.presentation.api.SuperkassaApi
import io.github.texport.superkassa.core.presentation.api.model.kkm.ReceiptBrandingResponse
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.print.model.Kept
import kz.mybrain.superkassa.domain.print.model.PrintSource
import kz.mybrain.superkassa.domain.print.port.PrintOut
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.settings.FakePrintOut
import kz.mybrain.superkassa.presentation.settings.settingsPorts
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.common.stringsOf
import kz.mybrain.superkassa.presentation.strings.print.printTexts
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Куда уходит нарисованная форма: в файл, на принтер, в закрытое окно.
 *
 * Принтер и диск — в памяти: проверка не печатает на принтере машины,
 * на которой идёт, и не пишет файлов.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrintOutputTest {

    private val core = FakeCore()
    private val signIn = SignIn()
    private val notices = Notices()
    private val out = FakePrintOut()
    private val app = CoreScene.app(core, signIn, notices, settings = settingsPorts().copy(printOut = out))
    private val texts = stringsOf(Language.Ru).preview

    @BeforeTest
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        core.on("getDocumentPrintPng") { FORM }
    }

    @AfterTest
    fun reset() = Dispatchers.resetMain()

    /**
     * Сохранённая форма называется по документу, откуда бы её ни открыли.
     *
     * Прежде чек из документов смены сохранялся под внутренним
     * идентификатором — такое имя покупателю ни о чём не говорит.
     */
    @Test
    fun `открытая по документу форма сохраняется под именем документа`() {
        signIn.enter(CoreScene.kkm(id = "kkm-1"), CoreScene.cashier(), CoreScene.PIN)
        core.on("getDocumentPrintPdf") { PDF }
        val model = printModel(app)
        val document = CoreScene.document("aae019ac-92e3").copy(shiftNo = 1, fiscalSign = "4178697373")
        model.actions().preview(document)
        model.saveShown()

        assertEquals("receipt-sale-shift-1-4178697373", model.state.value.savingName)
        assertEquals("receipt-sale-shift-1-4178697373.pdf", out.kept.single().first)
    }

    @Test
    fun `открытая форма печатается шириной ленты кассы на её принтер`() {
        val narrow = CoreScene.kkm(id = "kkm-1").copy(branding = ReceiptBrandingResponse(paperWidthMm = 58))
        signIn.enter(narrow, CoreScene.cashier(), CoreScene.PIN)
        app.areas.settings.printChoices.choosePrinter("kkm-1", "Чековый у кассы")
        val model = printModel(app)
        model.preview(PrintSource.Journal("d-1"), file = null)

        model.printShown()

        val job = out.printed.single()
        assertEquals("Чековый у кассы", job.printer)
        assertEquals(58, job.widthMm, "лента напечатана не шириной, заданной у кассы")
        assertEquals(Message.Done(texts.printSent), notices.last)
    }

    /**
     * Сохранять некуда — так и сказано.
     *
     * Устройство без файлов (Android) отвечало тем же, что передумавший
     * владелец, — молчанием, и владелец считал форму сохранённой.
     */
    @Test
    fun `сохранение там, где файлов нет, — отказ, а не молчание`() {
        signIn.enter(CoreScene.kkm(id = "kkm-1"), CoreScene.cashier(), CoreScene.PIN)
        core.on("getDocumentPrintPdf") { PDF }
        val nowhere = object : PrintOut by out {
            override suspend fun keep(bytes: ByteArray, name: String, title: String): Kept = Kept.Unavailable
        }
        val settings = settingsPorts().copy(printOut = nowhere)
        val model = printModel(CoreScene.app(core, signIn, notices, settings = settings))
        model.preview(PrintSource.Journal("d-1"), "receipt")
        model.saveShown()

        assertEquals(Message.Refusal(printTexts(Language.Ru).keepUnavailable, "NO_FILES"), notices.last)
    }

    /**
     * Принтера нет вовсе: «принтер не принял задание» было бы неправдой.
     *
     * И форму ради отказа касса не рисует: на Android рисование идёт через
     * WebView и занимает секунды, а отказ известен до него.
     */
    @Test
    fun `без принтеров печать говорит, что печатать некуда`() {
        out.names = emptyList()
        signIn.enter(CoreScene.kkm(id = "kkm-1"), CoreScene.cashier(), CoreScene.PIN)
        val model = printModel(app)

        model.print(PrintSource.Journal("d-1"))

        assertTrue(out.printed.isEmpty())
        assertTrue("getDocumentPrintPng" !in core.calls, "форму рисовали ради отказа")
        assertEquals(Message.Refusal(texts.printerMissing, "NO_PRINTER"), notices.last)
    }

    /** Окно закрыли, не дождавшись: запоздавшая форма его не открывает. */
    @Test
    fun `закрытое окно не открывается запоздавшей формой`() {
        signIn.enter(CoreScene.kkm(id = "kkm-1"), CoreScene.cashier(), CoreScene.PIN)
        val gate = CompletableDeferred<Unit>()
        val slow = object : Kassa {
            override suspend fun <T> call(request: (SuperkassaApi) -> T): T {
                gate.await()
                return request(core.api)
            }
        }
        val settings = settingsPorts().copy(printOut = out)
        val model = printModel(CoreScene.app(slow, signIn, notices, settings = settings))
        model.preview(PrintSource.Journal("d-1"), file = null)
        assertTrue(model.state.value.drawing, "окно открывается по нажатию, а не по готовой форме")

        model.close()
        gate.complete(Unit)

        assertNull(model.state.value.image, "запоздавшая форма открыла закрытое окно")
        assertFalse(model.state.value.drawing)
    }

    /**
     * Отказ называет связь, а не только следствие.
     *
     * Владелец смотрит документ кабинета и не знает, что печатную форму
     * по нему рисует касса на его же машине.
     */
    @Test
    fun `отказ предпросмотра называет и кабинет, и кассу`() {
        assertTrue(texts.noDrawer.contains("кабинет", ignoreCase = true), "не назван кабинет: ${texts.noDrawer}")
        assertTrue(texts.noDrawer.contains("касса", ignoreCase = true), "не названа касса: ${texts.noDrawer}")
    }

    private companion object {
        val FORM = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte())
        val PDF = "%PDF".encodeToByteArray()
    }
}
