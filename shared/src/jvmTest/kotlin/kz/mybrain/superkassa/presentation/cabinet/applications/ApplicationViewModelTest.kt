package kz.mybrain.superkassa.presentation.cabinet.applications

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kz.mybrain.superkassa.data.eds.NcaFake
import kz.mybrain.superkassa.data.eds.NcaReply
import kz.mybrain.superkassa.domain.cabinet.model.CabinetApplication
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRefusal
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.documents.ApplicationPrepared
import kz.mybrain.superkassa.domain.cabinet.port.CabinetApplications
import kz.mybrain.superkassa.domain.cabinet.unwired
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.presentation.cabinet.CabinetProblem
import kz.mybrain.superkassa.presentation.cabinet.CabinetScene
import kz.mybrain.superkassa.presentation.cabinet.onTestClock
import kz.mybrain.superkassa.presentation.cabinet.signingRig
import kz.mybrain.superkassa.presentation.common.message.Message
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Подача заявления моделью: итог и помеха — у вызвавшего, а не в общем поле.
 *
 * Прежде подача читала отказ из сеанса, и опрос соседней карточки успевал
 * стереть его раньше, чем экран его прочёл.
 */
class ApplicationViewModelTest {
    private var prepare: suspend () -> ApplicationPrepared = { error("not asked") }

    private val scene = CabinetScene().apply {
        ports.applications = object : CabinetApplications by unwired<CabinetApplications>() {
            override suspend fun prepare(application: CabinetApplication): ApplicationPrepared = prepare()
        }
    }

    @Test
    fun `отказ кабинета назван его кодом и словами`() = onTestClock {
        prepare = { throw CabinetRefusal("REGISTER_NOT_DRAFT", "Касса уже на учёте", httpStatus = 409) }
        val model = submitted()

        val outcome = assertIs<ApplicationOutcome.Failed>(model.state.value.outcome)
        assertEquals(CabinetProblem.Refused("REGISTER_NOT_DRAFT", "Касса уже на учёте"), outcome.problem)
        assertEquals("REGISTER_NOT_DRAFT", assertIs<Message.Refusal>(scene.app.services.talk.notices.last).code)
        assertFalse(scene.cabinet.state.value.busy)
        assertNull(model.state.value.stage, "после отказа на экране остался шаг подачи")
    }

    @Test
    fun `молчание кабинета названо молчанием`() = onTestClock {
        prepare = { throw IOException("no route") }
        val model = submitted()

        val outcome = assertIs<ApplicationOutcome.Failed>(model.state.value.outcome)
        assertIs<CabinetProblem.Unreachable>(outcome.problem)
    }

    /**
     * Отмена ожидания не отправляет заявление: подпись стоит между
     * подготовкой и отправкой, и в КГД к этому мигу ещё ничего не ушло.
     */
    @Test
    fun `отменённая подача ничего не отправляет в кабинет`(): Unit = inlineMain {
        runBlocking {
            NcaFake { NcaReply.Silence }.use { fake ->
                val asked = CopyOnWriteArrayList<String>()
                val cabinet = signingRig(fake, PREPARED, asked).enter()
                val application = ApplicationViewModel(cabinet.model)
                application.show(DRAFT)
                application.submit(DRAFT, here = null) {}
                withTimeout(WAIT) { while (fake.asked.isEmpty()) delay(STEP) }
                application.cancel()
                withTimeout(WAIT) { while (cabinet.model.state.value.busy) delay(STEP) }

                assertTrue(asked.any { it.endsWith("/registration/application") }, "заявление не готовилось: $asked")
                assertFalse(asked.any { it.endsWith("/registration/sign") }, "подписанное ушло в кабинет: $asked")
                assertNull(application.state.value.stage, "после отмены на экране остался шаг подачи")
            }
        }
    }

    /**
     * Пока заявление одной кассы ждёт кабинета, владелец открыл другую:
     * ни шаг, ни итог чужой подачи в её карточке не показываются, а своя
     * касса по возврату показывает свой отказ.
     */
    @Test
    fun `подача одной кассы не показывается в карточке другой`() = onTestClock {
        val answer = CompletableDeferred<Unit>()
        prepare = {
            answer.await()
            throw CabinetRefusal("ISNA_REJECTED", "ИСНА отклонила заявление", httpStatus = 409)
        }
        val model = submitted()
        model.show(OTHER)

        assertNull(model.state.value.stage, "шаг подачи первой кассы виден в карточке второй")
        answer.complete(Unit)
        assertNull(model.state.value.outcome, "отказ первой кассы виден в карточке второй")

        model.show(DRAFT)
        val outcome = assertIs<ApplicationOutcome.Failed>(model.state.value.outcome, "своя касса потеряла отказ")
        assertEquals(CabinetProblem.Refused("ISNA_REJECTED", "ИСНА отклонила заявление"), outcome.problem)
    }

    private fun submitted(): ApplicationViewModel = ApplicationViewModel(scene.cabinet).apply {
        show(DRAFT)
        submit(DRAFT, here = null) {}
    }

    private companion object {
        val DRAFT = CabinetRegister(id = "r-1", kkmId = 5_000_021, status = "DRAFT", factoryNumber = "SN-ECC-172758")
        val OTHER = CabinetRegister(id = "r-2", kkmId = 5_000_022, status = "DRAFT", factoryNumber = "SN-ECC-172759")
        const val PREPARED = """{"actionId":"a-1","actionType":"REGISTRATION","payloadToSign":"cGF5bG9hZA=="}"""
        val WAIT = 10.seconds
        val STEP = 20.milliseconds
    }
}
