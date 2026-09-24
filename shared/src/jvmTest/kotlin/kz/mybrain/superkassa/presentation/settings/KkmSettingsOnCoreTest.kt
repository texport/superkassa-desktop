package kz.mybrain.superkassa.presentation.settings

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmListParams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.settings.model.KkmSettingRules
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.settings.kkm.kkmSettingsModel
import kz.mybrain.superkassa.presentation.settings.receipt.ReceiptLine
import kz.mybrain.superkassa.presentation.settings.receipt.receiptFormModel
import kz.mybrain.superkassa.presentation.settings.tax.taxSettingsModel
import kz.mybrain.superkassa.presentation.shell.frame.shellModel
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Настройки самой кассы на настоящем ядре: владелец действует через модели
 * экрана, итог читается из кассы и из строки сообщений.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KkmSettingsOnCoreTest {
    private lateinit var desk: SettingsBench

    @BeforeTest
    fun open() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        desk = SettingsBench().enter()
    }

    @AfterTest
    fun close() {
        desk.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `новое название уходит в кассу и встаёт в шапку окна`() {
        val shell = shellModel(desk.app)
        val model = kkmSettingsModel(desk.app)

        model.typeName("Касса у окна")
        model.saveName()

        assertEquals("Касса у окна", desk.kassa.info().name)
        assertEquals("Касса у окна", shell.state.value.kkmName)
        assertIs<Message.Done>(desk.notices.last)
    }

    @Test
    fun `налог сохраняется в режиме программирования`() {
        val kkm = kkmSettingsModel(desk.app)
        val tax = taxSettingsModel(desk.app)
        kkm.switchProgramming()
        assertTrue(desk.kassa.info().isProgrammingMode, "режим программирования не включён")

        tax.chooseRegime("VAT_PAYER")
        tax.chooseVat("VAT_16")
        tax.saveTax()

        assertEquals("VAT_PAYER", desk.kassa.info().taxRegime)
        assertEquals("VAT_16", desk.kassa.info().defaultVatGroup)
        assertTrue(KkmSettingRules.met(KkmSettingRules.tax(desk.kassa.info())))
    }

    @Test
    fun `при открытой смене налог не принимается, и кнопка это знает до нажатия`() {
        desk.kassa.openShift()
        kkmSettingsModel(desk.app).switchProgramming()
        val tax = taxSettingsModel(desk.app)

        tax.chooseRegime("VAT_PAYER")
        tax.chooseVat("VAT_16")

        assertFalse(tax.state.value.savable, "кнопка сохранения налога горит при открытой смене")
        tax.saveTax()
        assertIs<Message.Refusal>(desk.notices.last)
        assertEquals("NO_VAT", desk.kassa.info().taxRegime ?: "NO_VAT")
    }

    @Test
    fun `автозакрытие и автоизъятие уходят в кассу`() {
        kkmSettingsModel(desk.app).switchProgramming()
        val tax = taxSettingsModel(desk.app)

        tax.switchAutoClose(true)
        tax.switchAutoCashout(true)

        assertTrue(desk.kassa.info().autoCloseShift)
        assertTrue(desk.kassa.info().autoCashout)
    }

    @Test
    fun `свои строки чека и ширина ленты уходят в кассу`() {
        kkmSettingsModel(desk.app).switchProgramming()
        val form = receiptFormModel(desk.app)

        form.typeLine(ReceiptLine.Footer, "Спасибо за покупку")
        form.saveLines()

        assertEquals("Спасибо за покупку", desk.kassa.info().branding?.footerMsg)
        assertIs<Message.Done>(desk.notices.last)
    }

    @Test
    fun `снятие с учёта после подтверждения удаляет кассу и уводит на выбор кассы`() {
        val model = kkmSettingsModel(desk.app)
        model.switchProgramming()

        model.askDecommission()
        assertTrue(model.state.value.decommissionAsked)
        model.decommission()

        assertTrue(desk.bench.api.listKkms(KkmListParams()).items.isEmpty(), "касса осталась в ядре")
        assertNull(desk.signIn.state.value.kkm, "окно осталось в настройках удалённой кассы")
    }

    @Test
    fun `снятие без режима программирования недоступно и касса его не примет`() {
        val needs = KkmSettingRules.decommission(desk.kassa.info())

        assertFalse(KkmSettingRules.met(needs))
        kkmSettingsModel(desk.app).decommission()
        assertEquals(1, desk.bench.api.listKkms(KkmListParams()).items.size)
        assertIs<Message.Refusal>(desk.notices.last)
    }

    @Test
    fun `в журнале нет ни пина, ни имени кассира, ни названия кассы`() {
        val model = kkmSettingsModel(desk.app)
        model.switchProgramming()
        model.typeName("Касса у окна")
        model.saveName()
        model.decommission()

        val journal = desk.journal.lines.joinToString("\n")
        listOf(SettingsBench.ADMIN_PIN, SettingsBench.CASHIER_NAME, SettingsBench.KKM_NAME, "Касса у окна").forEach {
            assertFalse(journal.contains(it), "в журнале «$it»:\n$journal")
        }
    }
}
