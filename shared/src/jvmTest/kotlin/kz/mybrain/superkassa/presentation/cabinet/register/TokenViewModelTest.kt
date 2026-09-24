package kz.mybrain.superkassa.presentation.cabinet.register

import io.github.texport.superkassa.core.domain.api.exception.PinLockedException
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRefusal
import kz.mybrain.superkassa.domain.cabinet.model.TokenIssued
import kz.mybrain.superkassa.domain.cabinet.port.CabinetRegisters
import kz.mybrain.superkassa.domain.cabinet.unwired
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.presentation.cabinet.CabinetScene
import kz.mybrain.superkassa.presentation.cabinet.onTestClock
import kz.mybrain.superkassa.presentation.common.message.Message
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Выпуск токена моделью: токен показан владельцу один раз, а касса этой
 * машины получает его сразу — или владелец узнаёт, почему не получила.
 */
class TokenViewModelTest {
    private var issue: () -> TokenIssued = { TokenIssued(kkmId = 5_000_021, token = 3_000_000_001L) }

    private val scene = CabinetScene().apply {
        ports.registers = object : CabinetRegisters by unwired<CabinetRegisters>() {
            override suspend fun issueToken(id: String): TokenIssued = issue()
        }
        app.services.signIn.enter(CoreScene.kkm(), CoreScene.cashier(), CoreScene.PIN)
    }

    @Test
    fun `токен показан беззнаковым и вписан в кассу этой машины`() = onTestClock {
        scene.core.on("updateOfdToken") { true }
        scene.core.on("getKkm") { CoreScene.kkm() }
        val model = TokenViewModel(scene.cabinet)

        model.issue("r-1", CoreScene.kkm())

        assertEquals(IssuedToken("r-1", 3_000_000_001L), model.state.value)
        assertIs<Message.Done>(scene.app.services.talk.notices.last)
    }

    @Test
    fun `запертый пин кассы назван, а выданный токен остаётся на экране`() = onTestClock {
        scene.core.on("updateOfdToken") { throw PinLockedException(retryAfterSeconds = 60) }
        val model = TokenViewModel(scene.cabinet)

        model.issue("r-1", CoreScene.kkm())

        assertEquals("PIN_LOCKED", assertIs<Message.Refusal>(scene.app.services.talk.notices.last).code)
        assertEquals(3_000_000_001L, model.state.value?.token, "выданный токен пропал: другого способа его узнать нет")
    }

    @Test
    fun `неверный пин кассы назван словами кассы`() = onTestClock {
        scene.core.refuse("updateOfdToken", "INVALID_PIN", ru = "Неверный пин")
        val model = TokenViewModel(scene.cabinet)

        model.issue("r-1", CoreScene.kkm())

        assertEquals(Message.Refusal("Неверный пин", "INVALID_PIN"), scene.app.services.talk.notices.last)
    }

    @Test
    fun `отказ кабинета в выпуске ничего не пишет в кассу`() = onTestClock {
        issue = { throw CabinetRefusal("TOKEN_NOT_ALLOWED", "По кассе подано заявление", httpStatus = 409) }
        val model = TokenViewModel(scene.cabinet)

        model.issue("r-1", CoreScene.kkm())

        assertNull(model.state.value)
        assertFalse("updateOfdToken" in scene.core.calls)
        assertEquals("TOKEN_NOT_ALLOWED", assertIs<Message.Refusal>(scene.app.services.talk.notices.last).code)
    }

    /**
     * Сервис приёма не подтвердил выпуск: токена нет — ни на экране,
     * ни в кассе. Прежде неподтверждённый ответ кабинета не читался вовсе,
     * а прочитанный ушёл бы в кассу нулём.
     */
    @Test
    fun `неподтверждённый токен не показан и не вписан в кассу`() = onTestClock {
        issue = { TokenIssued(kkmId = 5_000_021, token = null) }
        val model = TokenViewModel(scene.cabinet)

        model.issue("r-1", CoreScene.kkm())

        assertNull(model.state.value)
        assertFalse("updateOfdToken" in scene.core.calls)
        assertEquals("TOKEN_PENDING", assertIs<Message.Refusal>(scene.app.services.talk.notices.last).code)
    }
}
