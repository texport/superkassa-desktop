package kz.mybrain.superkassa.presentation.print.preview

import io.github.texport.superkassa.core.domain.api.exception.PinLockedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.print.model.PrintSource
import kz.mybrain.superkassa.domain.print.port.FakePrintOut
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.settings.settingsPorts
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Печатная форма без окна: кто её рисует, по какому пину и что видно.
 * Владелец смотрит чеки из кабинета и без выбранной кассы, и без пина.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrintViewModelTest {

    private val core = FakeCore()
    private val signIn = SignIn()
    private val notices = Notices()
    private val out = FakePrintOut()
    private val app = CoreScene.app(core, signIn, notices, settings = settingsPorts().copy(printOut = out))
    private val texts = textsOf(Language.Ru).common.preview

    /** Какими пинами касса рисовала форму, по порядку. */
    private val pins = mutableListOf<String>()

    /** Какие кассы рисовали форму, по порядку. */
    private val drawers = mutableListOf<String>()

    @BeforeTest
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        core.on("listKkms") { CoreScene.page(listOf(CoreScene.kkm(id = "a1", name = "Касса у входа"))) }
        core.on("getDocumentPrintPng") { args ->
            drawers += args[0] as String
            pins += args[2] as String
            FORM
        }
    }

    @AfterTest
    fun reset() = Dispatchers.resetMain()

    @Test
    fun `без вошедшего кассира рисует первая касса и спрашивает её пин`() {
        val model = printModel(app.services, app.areas.print)

        model.preview(PrintSource.Journal("d-1"), file = null)

        assertEquals("Касса у входа", model.state.value.pinFor, "владелец должен видеть, чей пин спрашивают")
        assertTrue(pins.isEmpty(), "без пина к кассе не ходят")
        model.enterPin("4827")
        assertNull(model.state.value.pinFor, "окно закрывается вводом")
        assertEquals(listOf("4827"), pins, "введённый пин продолжает ту же работу")
        assertContentEquals(FORM, model.state.value.image)
    }

    @Test
    fun `выбранная касса сильнее первой в списке`() {
        signIn.enter(CoreScene.kkm(id = "b2", name = "Касса в зале"), CoreScene.cashier(), CoreScene.PIN)
        val model = printModel(app.services, app.areas.print)

        model.preview(PrintSource.Journal("d-1"), file = null)

        assertEquals(listOf(CoreScene.PIN), pins, "вошедший кассир рисует своим пином, не спрашивая")
        assertEquals(listOf("b2"), drawers, "рисовала не выбранная касса")
        assertFalse("listKkms" in core.calls, "выбранная касса есть — искать первую незачем")
    }

    @Test
    fun `касс нет — об этом сказано словами`() {
        core.on("listKkms") { CoreScene.page(emptyList()) }
        val model = printModel(app.services, app.areas.print)

        model.preview(PrintSource.Journal("d-1"), file = null)

        assertEquals(Message.Done(texts.noDrawer), notices.last, "молчание здесь — тот же дефект, что и пустой экран")
        assertNull(model.state.value.pinFor)
    }

    @Test
    fun `введённый пин не считается входом кассира`() {
        val model = printModel(app.services, app.areas.print)
        model.preview(PrintSource.Journal("d-1"), file = null)
        model.enterPin("4827")

        assertFalse(signIn.state.value.signedIn, "разделы кассы от этого не открываются")
        assertEquals("", signIn.state.value.pin, "пин кассира остаётся пустым")
    }

    @Test
    fun `отказ по пину спрашивает его заново, а не повторяет неверный`() {
        core.refuse("getDocumentPrintPng", "USER_NOT_FOUND", ru = "Пользователь не найден")
        val model = printModel(app.services, app.areas.print)
        model.preview(PrintSource.Journal("d-1"), file = null)
        model.enterPin("0000")

        // Пин у касс разный, и на повторном вопросе владелец должен видеть,
        // к какой именно его спрашивают.
        assertEquals("Касса у входа", model.state.value.pinFor, "повторный вопрос не называет кассу")
        assertEquals(Message.Refusal("Пользователь не найден", "USER_NOT_FOUND"), notices.last)
    }

    /**
     * Документа нет — пин тут ни при чём.
     *
     * Окно ввода пина поверх такого отказа выдавало причину за неверный
     * пин, а верный пин стирало — следующая попытка начиналась с вопроса.
     */
    @Test
    fun `отказ не по пину оставляет введённый пин при себе`() {
        val model = printModel(app.services, app.areas.print)
        model.preview(PrintSource.Journal("d-1"), file = null)
        model.enterPin("4827")
        core.refuse("getDocumentPrintPng", "DOCUMENT_NOT_FOUND", ru = "Документ не найден")

        model.preview(PrintSource.Journal("d-2"), file = null)

        assertNull(model.state.value.pinFor, "окно пина поверх настоящей причины")
        assertEquals("Документ не найден", model.state.value.trouble?.words, "окно не назвало причину словами кассы")
        core.on("getDocumentPrintPng") { args -> FORM.also { pins += args[2] as String } }
        model.retry()
        assertEquals("4827", pins.last(), "верный пин стёрт чужим отказом")
    }

    /** Касса не смогла ответить — отказа по пину не было, и спрашивать его незачем. */
    @Test
    fun `сбой кассы не превращается в вопрос о пине`() {
        val model = printModel(app.services, app.areas.print)
        model.preview(PrintSource.Journal("d-1"), file = null)
        model.enterPin("4827")
        core.on("getDocumentPrintPng") { error("database is locked") }

        model.preview(PrintSource.Journal("d-2"), file = null)

        assertNull(model.state.value.pinFor, "сбой пином не лечится")
        assertNotNull(model.state.value.trouble, "окно закрылось вместе с формой")
        assertNull(model.state.value.trouble?.words, "сбою приписаны слова кассы")
        assertIs<Message.Failed>(notices.last)
    }

    /** Пин набирали неверно слишком часто: касса ждёт, и спрашивать сразу незачем. */
    @Test
    fun `заблокированный пин забывается, но заново сразу не спрашивается`() {
        val model = printModel(app.services, app.areas.print)
        model.preview(PrintSource.Journal("d-1"), file = null)
        model.enterPin("4827")
        core.on("getDocumentPrintPng") { throw PinLockedException(retryAfterSeconds = 60) }

        model.preview(PrintSource.Journal("d-2"), file = null)

        assertNull(model.state.value.pinFor, "окно пина открылось при заблокированном пине")
        assertEquals("PIN_LOCKED", (notices.last as Message.Refusal).code)
        core.on("getDocumentPrintPng") { FORM }
        model.retry()
        assertNotNull(model.state.value.pinFor, "заблокированный пин остался в памяти")
    }

    @Test
    fun `выход кассира забывает пин рисовальщика`() {
        signIn.enter(CoreScene.kkm(id = "a1"), CoreScene.cashier(), CoreScene.PIN)
        signIn.signOut()
        val model = printModel(app.services, app.areas.print)
        model.preview(PrintSource.Journal("d-1"), file = null)
        model.enterPin("4827")

        signIn.enter(CoreScene.kkm(id = "a1"), CoreScene.cashier(), CoreScene.PIN)
        signIn.signOut()
        model.preview(PrintSource.Journal("d-2"), file = null)

        assertNotNull(model.state.value.pinFor, "после выхода пин спрашивается заново")
    }

    private companion object {
        val FORM = byteArrayOf(0x89.toByte(), 'P'.code.toByte(), 'N'.code.toByte(), 'G'.code.toByte())
    }
}
