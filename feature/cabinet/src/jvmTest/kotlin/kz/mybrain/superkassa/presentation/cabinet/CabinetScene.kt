package kz.mybrain.superkassa.presentation.cabinet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.cabinet.TestAccount
import kz.mybrain.superkassa.domain.cabinet.TestPorts
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.domain.cabinet.port.CabinetAccount
import kz.mybrain.superkassa.domain.cabinet.port.CabinetPlaces
import kz.mybrain.superkassa.domain.cabinet.port.CabinetRegisters
import kz.mybrain.superkassa.domain.cabinet.unwired
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.services
import kz.mybrain.superkassa.presentation.common.model.WindowServices

/**
 * Модель кабинета окна поверх тестовых портов и подставной кассы.
 *
 * Обращения к портам не уходят в сеть, а главный поток модели — часы
 * проверки: пятисекундный опрос проверяется без пяти секунд ожидания.
 */
internal class CabinetScene(val ports: TestPorts = TestPorts(), val core: FakeCore = FakeCore()) {
    val services: WindowServices = CoreScene.services(core)
    val cabinet: CabinetViewModel by lazy { CabinetViewModel(cabinetCases(services, ports), services.talk) }
}

/** Проверка на часах проверки: модели окна работают в её главном потоке. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun onTestClock(block: suspend TestScope.() -> Unit) = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    try {
        block()
    } finally {
        Dispatchers.resetMain()
    }
}

/** Вход, который кабинет или подписывающий не пропускает: [failure] — чем именно. */
internal fun refusing(failure: Exception): CabinetAccount = object : CabinetAccount by TestAccount() {
    override suspend fun signIn() = throw failure
}

/** Компания без точек и касс: владелец вошёл, и читать в хозяйстве нечего. */
internal fun TestPorts.emptyCompany() {
    places = object : CabinetPlaces by unwired<CabinetPlaces>() {
        override suspend fun all(onPart: (List<RetailPlace>, Long) -> Unit): List<RetailPlace> = emptyList()
    }
    registers = object : CabinetRegisters by unwired<CabinetRegisters>() {
        override suspend fun all(onPart: (List<CabinetRegister>, Long) -> Unit): List<CabinetRegister> = emptyList()

        override suspend fun blocked(): Set<String> = emptySet()
    }
}
