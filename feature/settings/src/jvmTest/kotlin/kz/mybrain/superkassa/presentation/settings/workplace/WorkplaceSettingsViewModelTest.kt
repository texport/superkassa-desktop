package kz.mybrain.superkassa.presentation.settings.workplace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.settings.port.MemoryChoices
import kz.mybrain.superkassa.domain.settings.port.settingsPorts
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.workplace.model.MapServices
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.services
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Настройки самой машины без окна: адреса служб, отрасль и принтер кассы.
 *
 * Хранит их рабочее место; здесь оно в памяти, и файлы машины не трогаются.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkplaceSettingsViewModelTest {

    private val signIn = SignIn()
    private val machine = MemoryChoices()
    private val services = CoreScene.services(FakeCore(), signIn, memory = machine.memory)
    private val settings = settingsPorts().copy(workplace = machine).settings

    @BeforeTest
    fun main() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun reset() = Dispatchers.resetMain()

    /** Адрес без схемы приложение не разберёт вовсе: такой не сохраняется. */
    @Test
    fun `негодный адрес кабинета не уходит в настройки`() {
        val model = workplaceSettingsModel(services, settings)

        model.typeCabinet("192.168.50.35:17700")
        assertTrue(model.state.value.cabinetMalformed)
        model.saveCabinet()

        assertEquals("https://bfd-cabinet.ecc.kz", machine.cabinetUrl)
    }

    @Test
    fun `годный адрес сохраняется без краёв и черты на конце`() {
        val model = workplaceSettingsModel(services, settings)

        model.typeCabinet("  http://192.168.50.35:17700/  ")
        model.saveCabinet()

        assertEquals("http://192.168.50.35:17700", machine.cabinetUrl)
        assertNull(model.state.value.cabinetDraft)
    }

    @Test
    fun `службы карты сохраняются и возвращаются к общедоступным`() {
        val model = workplaceSettingsModel(services, settings)

        model.typeMaps(MapServices(tiles = " https://tiles.example.kz/{z}/{x}/{y}.png ", search = ""))
        model.saveMaps()
        assertEquals(MapServices(tiles = "https://tiles.example.kz/{z}/{x}/{y}.png"), machine.maps)

        model.resetMaps()
        assertEquals(MapServices(), machine.maps)
        assertFalse(model.state.value.mapsCustom)
    }

    /** Отрасль держится за кассой: на одной машине их бывает несколько. */
    @Test
    fun `отрасль помнится за каждой кассой`() {
        signIn.enter(CoreScene.kkm(id = "kkm-1"), CoreScene.cashier(), CoreScene.PIN)
        val model = workplaceSettingsModel(services, settings)
        model.chooseDomain("DOMAIN_TAXI")

        signIn.enter(CoreScene.kkm(id = "kkm-2"), CoreScene.cashier(), CoreScene.PIN)

        assertNull(model.state.value.domainCode)
        assertEquals("DOMAIN_TAXI", machine.memory.domain("kkm-1"))
    }
}
