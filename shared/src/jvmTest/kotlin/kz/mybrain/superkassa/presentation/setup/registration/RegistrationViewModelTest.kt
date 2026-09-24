package kz.mybrain.superkassa.presentation.setup.registration

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.setup.model.CabinetRecord
import kz.mybrain.superkassa.domain.setup.port.FakeSetupCabinet
import kz.mybrain.superkassa.domain.setup.port.SetupPorts
import kz.mybrain.superkassa.kassa.MemorySetup
import kz.mybrain.superkassa.presentation.setup.DirectCalls
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Шаг постановки на учёт без окна: касса в кабинете, заявление и его подпись.
 *
 * Прежде шаг вела разметка прямо клиентом кабинета, и проверить его можно
 * было только снимком.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationViewModelTest {
    private val cabinet = FakeSetupCabinet()
    private val calls = DirectCalls()

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    private fun model(): RegistrationViewModel = registrationModel(SetupPorts(MemorySetup(), cabinet), calls)

    @Test
    fun `касса на учёте открывает последний шаг`() {
        val model = model()
        model.readRecord(REGISTER)
        assertFalse(model.state.value.onRecord(REGISTER), "черновик в кабинете назван поставленным на учёт")

        cabinet.record = CabinetRecord("REGISTERED", "000000200042")
        model.readRecord(REGISTER)

        assertTrue(model.state.value.onRecord(REGISTER))
        assertEquals(listOf("record $REGISTER", "record $REGISTER"), cabinet.asked)
    }

    /** Мастер, начатый заново, заводит другую кассу: прочитанное о прежней ей не в счёт. */
    @Test
    fun `прочитанное о другой кассе в счёт не идёт`() {
        cabinet.record = CabinetRecord("REGISTERED", "000000200042")
        val model = model()
        model.readRecord(REGISTER)

        assertFalse(model.state.value.onRecord("r-2"))
        assertFalse(model.state.value.onRecord(null))
        assertNull(model.state.value.recordOf("r-2"))
    }

    /** Поданное перечитывает кассу: ответ ИСНА ждут на том же шаге. */
    @Test
    fun `поданное заявление перечитывает кассу`() {
        model().submit(REGISTER)

        val sent = "send $REGISTER ${FakeSetupCabinet.ACTION} signed-${FakeSetupCabinet.PAYLOAD}"
        assertEquals(listOf("prepare $REGISTER", sent, "record $REGISTER"), cabinet.asked)
    }

    /** Прерванная подпись ничего не отправляет и снимает срок подписи; второе нажатие не подаёт дважды. */
    @Test
    fun `прерванная подпись оставляет заявление черновиком`() {
        val asked = CompletableDeferred<Unit>()
        cabinet.signer = {
            asked.complete(Unit)
            awaitCancellation()
        }
        val model = model()

        model.submit(REGISTER)
        assertTrue(asked.isCompleted && model.state.value.signing, "подпись не попрошена")
        model.submit(REGISTER)
        model.cancelSubmit()

        assertFalse(model.state.value.signing, "срок подписи остался после отмены")
        assertEquals(listOf("prepare $REGISTER"), cabinet.asked, "второе нажатие или отмена что-то отправили")
    }

    @Test
    fun `помеха подписи показана, а шаг не занят`() {
        cabinet.signer = { error("NCALayer silent") }
        val model = model()

        model.submit(REGISTER)

        assertEquals(listOf("submit registration: NCALayer silent"), calls.failed)
        assertFalse(model.state.value.signing)
    }

    private companion object {
        const val REGISTER = "r-1"
    }
}
