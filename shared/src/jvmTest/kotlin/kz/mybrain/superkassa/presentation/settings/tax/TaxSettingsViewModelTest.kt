package kz.mybrain.superkassa.presentation.settings.tax

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.TaxRegime
import io.github.texport.superkassa.core.presentation.api.model.kkm.VatGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.settings.SettingsScene
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.common.stringsOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Налоги кассы без окна: справочники, условия кассы и сохранение.
 *
 * Касса принимает налоги только в режиме программирования, при закрытой
 * смене и пустой очереди; кнопка обязана гаснуть ровно по этим условиям.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TaxSettingsViewModelTest {

    private val core = FakeCore()
    private val signIn = SignIn()
    private val notices = Notices()
    private val app = CoreScene.app(core, signIn, notices)
    private val texts = stringsOf(Language.Ru).settings

    /** Что касса получила последним обращением к налогам. */
    private var sent: List<Any?> = emptyList()

    @BeforeTest
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        core.on("getTaxRegimes") { SettingsScene.REGIMES }
        core.on("listVatRates") { SettingsScene.RATES }
        core.on("updateTaxSettings") { args ->
            sent = args
            programming().copy(taxRegime = (args[2] as TaxRegime).name, defaultVatGroup = (args[3] as VatGroup).name)
        }
    }

    @AfterTest
    fun reset() = Dispatchers.resetMain()

    private fun programming(): KkmResponse =
        CoreScene.kkm().copy(isProgrammingMode = true, taxRegime = "NO_VAT", defaultVatGroup = "NO_VAT")

    @Test
    fun `режим и ставка уходят в кассу одним обращением`() {
        signIn.enter(programming(), CoreScene.cashier(), CoreScene.PIN)
        val model = taxSettingsModel(app)

        model.chooseRegime("VAT_PAYER")
        model.chooseVat("VAT_12")
        assertTrue(model.state.value.savable)
        model.saveTax()

        assertEquals(listOf("kkm-1", CoreScene.PIN, TaxRegime.VAT_PAYER, VatGroup.VAT_12), sent)
        assertEquals("VAT_PAYER", signIn.state.value.kkm?.taxRegime, "касса не узнала нового режима")
        assertNull(model.state.value.regimeDraft, "выбранное не забылось после согласия кассы")
        assertEquals(Message.Done(texts.settingsSaved), notices.last)
    }

    /** У неплательщика НДС выбирать ставку не из чего. */
    @Test
    fun `без НДС ставка по умолчанию тоже без НДС`() {
        val payer = programming().copy(taxRegime = "VAT_PAYER", defaultVatGroup = "VAT_12")
        signIn.enter(payer, CoreScene.cashier(), CoreScene.PIN)
        val model = taxSettingsModel(app)

        model.chooseRegime(TaxRegime.NO_VAT.name)

        assertEquals(VatGroup.NO_VAT.name, model.state.value.vatGroup)
        assertFalse(model.state.value.vatChoosable)
    }

    @Test
    fun `открытая смена и непустая очередь гасят сохранение`() {
        signIn.enter(programming().copy(isShiftOpen = true), CoreScene.cashier(), CoreScene.PIN)
        val model = taxSettingsModel(app)
        model.chooseRegime("VAT_PAYER")
        assertFalse(model.state.value.savable, "налоги предлагаются к сохранению при открытой смене")

        signIn.refresh(programming().copy(offlineQueueCount = 2))
        assertFalse(model.state.value.savable, "налоги предлагаются к сохранению при непустой очереди")
    }

    /** Касса не ответила на справочники: на месте полей — беда с повтором, повтор дочитывает. */
    @Test
    fun `непрочитанные справочники дочитываются повтором`() {
        core.on("getTaxRegimes") { error("database is locked") }
        signIn.enter(programming(), CoreScene.cashier(), CoreScene.PIN)
        val model = taxSettingsModel(app)
        assertTrue(model.state.value.dictionariesMissing)

        core.on("getTaxRegimes") { SettingsScene.REGIMES }
        model.retryDictionaries()

        assertFalse(model.state.value.dictionariesMissing)
    }

    @Test
    fun `отказ кассы оставляет выбранное`() {
        core.refuse("updateTaxSettings", "SHIFT_OPEN", ru = "Сначала закройте смену")
        signIn.enter(programming(), CoreScene.cashier(), CoreScene.PIN)
        val model = taxSettingsModel(app)

        model.chooseRegime("VAT_PAYER")
        model.chooseVat("VAT_12")
        model.saveTax()

        assertEquals(Message.Refusal("Сначала закройте смену", "SHIFT_OPEN"), notices.last)
        assertEquals("VAT_PAYER", model.state.value.regime)
    }

    /** Автозакрытие и автоизъятие касса меняет одним обращением: второе значение уходит как есть. */
    @Test
    fun `автозакрытие уходит вместе с автоизъятием`() {
        core.on("updateKkmSettings") { args ->
            sent = args
            programming().copy(autoCloseShift = args[2] as Boolean, autoCashout = args[3] as Boolean)
        }
        signIn.enter(programming().copy(autoCashout = true), CoreScene.cashier(), CoreScene.PIN)
        val model = taxSettingsModel(app)

        model.switchAutoClose(true)

        assertEquals(listOf("kkm-1", CoreScene.PIN, true, true), sent)
        assertTrue(signIn.state.value.kkm?.autoCloseShift == true)
    }
}
