package kz.mybrain.superkassa.presentation.kassa.sale

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.kassa.port.FixedDeliverySetup
import kz.mybrain.superkassa.domain.kassa.port.KassaPorts
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.MemoryWorkplace
import kz.mybrain.superkassa.kassa.SaleScene
import kz.mybrain.superkassa.kassa.SaleScene.receipts
import kz.mybrain.superkassa.kassa.services
import kz.mybrain.superkassa.presentation.common.message.Notices
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Переключатели «Что открыто при входе в продажу» действуют на экран продажи.
 *
 * Владелец: «по-моему некоторые не работают». Переключатель ручного ввода
 * и правда не работал — у нового чека ввод раскрывается всегда, — и его
 * в настройках больше нет. Каждый оставшийся выключают в карточке
 * настроек и открывают продажу: раздел свёрнут; включают — развёрнут.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SalePanelSettingsTest {
    private val memory = MemoryWorkplace()

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    private fun opened(): SaleViewModel {
        val core = SaleScene.core().also { it.receipts() }
        val services = CoreScene.services(core, SaleScene.signedIn(), Notices(), memory)
        return saleModel(services, KassaPorts(FixedDeliverySetup())).also { it.visit() }
    }

    @Test
    fun `каждый переключатель настроек сворачивает и разворачивает свой раздел продажи`() {
        val settings = SalePanels(memory)
        configurable.forEach { panel ->
            settings.toggle(panel)
            val off = opened().state.value.expanded(panel)
            assertFalse(off, "$panel: выключен в настройках, а продажа его развернула")
            settings.toggle(panel)
            val on = opened().state.value.expanded(panel)
            assertTrue(on, "$panel: включён в настройках, а продажа его свернула")
        }
    }

    @Test
    fun `свёрнутое на экране продажи видно в настройках`() {
        val model = opened()
        model.togglePanel(SalePanel.Money)
        assertFalse(SalePanels(memory).expanded(SalePanel.Money), "настройки не знают, что оплату свернули")
    }

    @Test
    fun `ручного ввода в настройках нет — у нового чека он открыт всегда`() {
        assertEquals(null, SalePanel.PositionEntry.setting)
        assertEquals(SalePanel.entries - SalePanel.PositionEntry, configurable)
    }

    private val configurable = SalePanel.entries.filter { it.setting != null }
}
