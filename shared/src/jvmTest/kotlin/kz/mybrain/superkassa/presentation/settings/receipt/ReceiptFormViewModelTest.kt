package kz.mybrain.superkassa.presentation.settings.receipt

import io.github.texport.superkassa.core.presentation.api.model.kkm.ReceiptBrandingRequest
import io.github.texport.superkassa.core.presentation.api.model.kkm.ReceiptBrandingResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.ReceiptLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Печатная форма кассы без окна: правка одного поля не сбрасывает прочие.
 *
 * Касса меняет оформление целиком, и правка, не несущая остальных полей,
 * вернула бы их к умолчаниям.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReceiptFormViewModelTest {

    private val core = FakeCore()
    private val signIn = SignIn()
    private val notices = Notices()
    private val app = CoreScene.app(core, signIn, notices)
    private var sent: ReceiptBrandingRequest? = null

    private val branded = CoreScene.kkm(state = "PROGRAMMING").copy(
        isProgrammingMode = true,
        branding = ReceiptBrandingResponse(language = ReceiptLanguage.KK, paperWidthMm = 58, footerMsg = "Рахмет!")
    )

    @BeforeTest
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        core.on("getPaperWidths") { emptyList<Any>() }
        core.on("updateBrandingSettings") { args ->
            sent = args[2] as ReceiptBrandingRequest
            branded
        }
        signIn.enter(branded, CoreScene.cashier(), CoreScene.PIN)
    }

    @AfterTest
    fun reset() = Dispatchers.resetMain()

    @Test
    fun `свои строки уходят вместе с прочим оформлением`() {
        val model = receiptFormModel(app)

        model.typeLine(ReceiptLine.Header, "Магазин у дома")
        model.saveLines()

        assertEquals("Магазин у дома", sent?.headerMsg)
        assertEquals("Рахмет!", sent?.footerMsg, "нетронутая строка сброшена")
        assertEquals(58, sent?.paperWidthMm, "ширина ленты сброшена правкой строк")
        assertTrue(model.state.value.lineDrafts.isEmpty())
    }

    @Test
    fun `полная страница уходит нулём ширины`() {
        receiptFormModel(app).chooseLayout("FULLSCREEN")

        assertEquals(0, sent?.paperWidthMm)
        assertEquals(ReceiptLanguage.KK, sent?.language)
    }

    @Test
    fun `отказ кассы оставляет набранные строки`() {
        core.refuse("updateBrandingSettings", "KKM_NOT_PROGRAMMING", ru = "Касса не в режиме программирования")
        val model = receiptFormModel(app)

        model.typeLine(ReceiptLine.Footer, "Спасибо")
        model.saveLines()

        assertEquals(Message.Refusal("Касса не в режиме программирования", "KKM_NOT_PROGRAMMING"), notices.last)
        assertEquals("Спасибо", model.state.value.lineDrafts[ReceiptLine.Footer])
    }

    /** Набранные строки прежде сбрасывались при смене кассы; теперь у каждой кассы свои. */
    @Test
    fun `набранные строки у каждой кассы свои и переживают смену кассы`() {
        val model = receiptFormModel(app)
        model.typeLine(ReceiptLine.Header, "Магазин у дома")

        signIn.enter(branded.copy(kkmId = "kkm-2"), CoreScene.cashier(), CoreScene.PIN)
        assertTrue(model.state.value.lineDrafts.isEmpty(), "строки первой кассы видны у второй")
        model.typeLine(ReceiptLine.Footer, "Сау болыңыз")
        signIn.enter(branded, CoreScene.cashier(), CoreScene.PIN)

        assertEquals(mapOf(ReceiptLine.Header to "Магазин у дома"), model.state.value.lineDrafts)
        signIn.enter(branded.copy(kkmId = "kkm-2"), CoreScene.cashier(), CoreScene.PIN)
        assertEquals(mapOf(ReceiptLine.Footer to "Сау болыңыз"), model.state.value.lineDrafts)
    }
}
