package kz.mybrain.superkassa.domain.cabinet.usecase.register

import io.github.texport.superkassa.core.domain.api.exception.PinLockedException
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Выданный токен вписывается в кассу этой машины — ядром, её пином.
 *
 * Касса, не знающая нового токена, на первом же чеке получила бы от БФД
 * «неверный токен». Отказ кассы доходит до владельца её кодом и словами:
 * по нему видно, что токен выдан, но в кассу не лёг.
 */
class WriteTokenHereTest {
    private val core = FakeCore()
    private val signIn = SignIn().apply { enter(CoreScene.kkm(), CoreScene.cashier(), CoreScene.PIN) }
    private val write = WriteTokenHere(core.kassa(), signIn)

    @Test
    fun `токен уходит в кассу её пином, и касса перечитывается`(): Unit = runBlocking {
        var asked: List<Any?> = emptyList()
        core.on("updateOfdToken") { args -> true.also { asked = args } }
        core.on("getKkm") { CoreScene.kkm(name = "Касса у окна") }

        val written = write(CoreScene.kkm(), TOKEN)

        assertEquals(listOf("kkm-1", CoreScene.PIN, TOKEN), asked)
        assertEquals("Касса у окна", (written as Answer.Done).value.name)
        assertEquals("Касса у окна", signIn.state.value.kkm?.name, "вход остался с прежней кассой")
    }

    @Test
    fun `в чужую кассу токен не пишется`(): Unit = runBlocking {
        assertNull(write(CoreScene.kkm(id = "kkm-2"), TOKEN))
        assertNull(write(null, TOKEN))
        assertFalse("updateOfdToken" in core.calls, "касса, в которую не вошли, получила токен")
    }

    @Test
    fun `отказ кассы доходит её кодом и словами`(): Unit = runBlocking {
        core.refuse(
            "updateOfdToken",
            "KKM_BLOCKED",
            ru = "Касса заблокирована",
            kk = "Касса бұғатталған",
            en = "Blocked"
        )

        val written = write(CoreScene.kkm(), TOKEN)

        assertEquals(Answer.Refused("KKM_BLOCKED", "Касса заблокирована", "Касса бұғатталған", "Blocked"), written)
        assertFalse("getKkm" in core.calls, "после отказа касса перечитывалась")
    }

    @Test
    fun `неверный пин назван отказом, а не сбоем`(): Unit = runBlocking {
        core.refuse("updateOfdToken", "INVALID_PIN", ru = "Неверный пин")

        assertEquals("INVALID_PIN", assertIs<Answer.Refused>(write(CoreScene.kkm(), TOKEN)).code)
    }

    @Test
    fun `запертый пин назван своим кодом`(): Unit = runBlocking {
        core.on("updateOfdToken") { throw PinLockedException(retryAfterSeconds = 60) }

        assertEquals("PIN_LOCKED", assertIs<Answer.Refused>(write(CoreScene.kkm(), TOKEN)).code)
    }

    @Test
    fun `сбой кассы не выдаётся за записанный токен`(): Unit = runBlocking<Unit> {
        core.on("updateOfdToken") { error("database is gone") }

        assertIs<Answer.Failed>(write(CoreScene.kkm(), TOKEN))
    }

    private companion object {
        const val TOKEN = "3000000001"
    }
}
