package kz.mybrain.superkassa.presentation.cabinet

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kz.mybrain.superkassa.data.eds.NcaFake
import kz.mybrain.superkassa.data.eds.NcaReply
import kz.mybrain.superkassa.domain.cabinet.TestAccount
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRefusal
import kz.mybrain.superkassa.domain.cabinet.model.EdsProblem
import kz.mybrain.superkassa.domain.cabinet.model.EdsRefusal
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Вход владельца в кабинет моделью окна: занятость, отсчёт и помеха.
 *
 * Отказ кабинета показывается его словами; отмена подписи самим
 * владельцем не показывается вовсе — он её и вызвал.
 */
class CabinetViewModelTest {

    @Test
    fun `отказ кабинета при входе показан его словами, и окно свободно`() = onTestClock {
        val refusal = CabinetRefusal("EDS_INVALID", "Подпись недействительна", httpStatus = 401)
        val scene = CabinetScene().apply { ports.account = refusing(refusal) }

        scene.cabinet.signIn()

        assertEquals(Message.Refusal("Подпись недействительна", "EDS_INVALID"), scene.app.notices.last)
        assertFalse(scene.cabinet.state.value.open)
        assertFalse(scene.cabinet.state.value.busy)
        assertNull(scene.cabinet.state.value.signingSince, "отсчёт остался после отказа")
    }

    @Test
    fun `владелец вошёл — окно открыто`() = onTestClock {
        val scene = CabinetScene().apply { ports.emptyCompany() }

        scene.cabinet.signIn()

        assertTrue(scene.cabinet.state.value.open)
        assertEquals(TestAccount.OWNER, scene.cabinet.state.value.owner)
    }

    @Test
    fun `незапущенный NCALayer назван, а не выдан за отказ кабинета`() = onTestClock {
        val scene = CabinetScene().apply { ports.account = refusing(EdsRefusal(EdsProblem.Unreachable, "refused")) }

        scene.cabinet.signIn()

        val shown = assertIs<Message.Refusal>(scene.app.notices.last)
        assertEquals(textsOf(Language.Ru).cabinet.noNcaLayer, shown.text)
        assertFalse(scene.cabinet.state.value.busy)
    }

    /**
     * Отмена посреди ожидания: сеанс кабинета не остаётся занятым
     * и молчит о помехе — владелец сам её и вызвал.
     */
    @Test
    fun `отменённый вход не оставляет сеанс занятым`(): Unit = inlineMain {
        runBlocking {
            NcaFake { NcaReply.Silence }.use { fake ->
                val cabinet = signingRig(fake, CHALLENGE).model
                cabinet.signIn()
                withTimeout(WAIT) { while (fake.asked.isEmpty()) delay(STEP) }
                assertTrue(cabinet.state.value.busy, "ожидание подписи не показано занятостью")
                cabinet.cancelSignIn()
                withTimeout(WAIT) { while (cabinet.state.value.busy) delay(STEP) }

                assertNull(cabinet.state.value.signingSince, "отсчёт остался на экране после отмены")
                assertNull(cabinet.talk.notices.last, "по отмене владельцу ничего не показывается")
            }
        }
    }

    private companion object {
        const val CHALLENGE = """{"challengeId":"c-1","payload":"cGF5bG9hZA=="}"""
        val WAIT = 10.seconds
        val STEP = 20.milliseconds
    }
}
