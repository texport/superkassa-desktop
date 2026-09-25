package kz.mybrain.superkassa.presentation.cabinet.register

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRefusal
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.RegisterEdit
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegisterState
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegistrationAction
import kz.mybrain.superkassa.domain.cabinet.port.CabinetApplications
import kz.mybrain.superkassa.domain.cabinet.port.CabinetRegisters
import kz.mybrain.superkassa.domain.cabinet.unwired
import kz.mybrain.superkassa.presentation.cabinet.CabinetScene
import kz.mybrain.superkassa.presentation.cabinet.onTestClock
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Карточка кассы, пока заявление в ИСНА, перечитывается сама — раз в пять секунд.
 *
 * Опрос шёл тем же путём, что и действия владельца, и первое же его
 * завершение снимало занятость посреди подписи и стирало отказ, который
 * владелец ещё читал.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {
    private val reads = mutableListOf<String>()
    private val edited = CompletableDeferred<Unit>()
    private var cardAnswer: () -> CabinetRegister = { WAITING }

    /** Чем кабинет отвечает на чтение кассы; `null` — отвечает. */
    private var silence: Exception? = null

    private val registers = object : CabinetRegisters by unwired<CabinetRegisters>() {
        override suspend fun one(id: String): CabinetRegister = cardAnswer().also { reads += id }

        override suspend fun state(id: String): RegisterState {
            silence?.let { throw it }
            return RegisterState(cashRegisterId = id, businessStatus = WAITING.status)
        }

        override suspend fun rename(id: String, name: String): CabinetRegister =
            throw CabinetRefusal("NAME_TOO_LONG", "Название длиннее допустимого", httpStatus = 422)

        override suspend fun edit(id: String, edit: RegisterEdit): CabinetRegister {
            edited.await()
            return WAITING
        }
    }

    private val applications = object : CabinetApplications by unwired<CabinetApplications>() {
        override suspend fun actions(registerId: String): List<RegistrationAction> =
            silence?.let { throw it } ?: emptyList()
    }

    private val scene = CabinetScene().apply {
        ports.registers = registers
        ports.applications = applications
    }

    @Test
    fun `опрос ИСНА не снимает занятость и не стирает показанный отказ`() = onTestClock {
        val model = RegisterViewModel(scene.cabinet)
        model.show(WAITING)
        reads.clear()
        model.rename("Касса у окна с очень длинным названием")
        val refusal = assertIs<Message.Refusal>(scene.services.talk.notices.last)
        model.restamp("SN-ECC-172759")
        assertTrue(scene.cabinet.state.value.busy, "правка владельца не заняла окно")

        advanceTimeBy(POLL)
        runCurrent()

        assertTrue(reads.isNotEmpty(), "опроса не было")
        assertTrue(scene.cabinet.state.value.busy, "опрос снял занятость посреди действия владельца")
        assertEquals(refusal, scene.services.talk.notices.last, "опрос стёр показанный отказ")
        edited.complete(Unit)
        runCurrent()
        assertFalse(scene.cabinet.state.value.busy, "занятость осталась после правки")
        model.hide()
    }

    @Test
    fun `опрос о помехе молчит`() = onTestClock {
        val model = RegisterViewModel(scene.cabinet)
        model.show(WAITING)
        cardAnswer = { throw IOException("no route") }

        advanceTimeBy(POLL)
        runCurrent()

        val shown = scene.services.talk.notices.last
        assertEquals(null, shown, "владельцу показана помеха опроса, которого он не просил")
        model.hide()
    }

    @Test
    fun `молчание кабинета при чтении владельцем названо словами кабинета`() = onTestClock {
        silence = IOException("no route")
        cardAnswer = { throw IOException("no route") }
        val model = RegisterViewModel(scene.cabinet)

        model.show(WAITING.copy(status = "REGISTERED"))

        val refusal = assertIs<Message.Refusal>(scene.services.talk.notices.last)
        assertEquals(textsOf(Language.Ru).cabinet.unreachable, refusal.text)
        assertFalse(scene.cabinet.state.value.busy)
    }

    private companion object {
        val WAITING = CabinetRegister(id = "r-1", kkmId = 5_000_021, status = "REGISTRATION_IN_ISNA_PROCESS")
        val POLL = 5.seconds + 1.seconds
    }
}
