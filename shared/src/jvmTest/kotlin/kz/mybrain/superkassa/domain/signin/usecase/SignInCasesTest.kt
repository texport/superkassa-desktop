package kz.mybrain.superkassa.domain.signin.usecase

import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.MemoryWorkplace
import kz.mybrain.superkassa.kassa.SilentJournal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Вход, уход и смена кассы без экрана: касса по заказу и держатель входа.
 *
 * Касса и пин команды берутся у держателя входа в момент вызова:
 * без вошедшего кассира команда кассе не уходит вовсе.
 */
class SignInCasesTest {
    private val core = FakeCore()
    private val signIn = SignIn()
    private val memory = MemoryWorkplace()

    private fun enter() = signIn.enter(CoreScene.kkm(), CoreScene.cashier(), CoreScene.PIN)

    @Test
    fun `принятый пин записывает вход и запоминает кассу`(): Unit = runBlocking {
        core.on("authenticate") { CoreScene.cashier() }

        val answer = SignInCashier(core.kassa(), signIn, memory, SilentJournal)(CoreScene.kkm(), CoreScene.PIN)

        assertIs<Answer.Done<*>>(answer)
        assertTrue(signIn.state.value.signedIn)
        assertEquals("kkm-1", memory.rememberedKkmId)
    }

    @Test
    fun `отказ в пине входа не записывает`(): Unit = runBlocking {
        core.refuse("authenticate", "INVALID_PIN")

        SignInCashier(core.kassa(), signIn, memory, SilentJournal)(CoreScene.kkm(), "9999")

        assertFalse(signIn.state.value.signedIn)
        assertNull(memory.rememberedKkmId)
    }

    @Test
    fun `уход кассира оставляет кассу, смена кассы — нет`(): Unit = runBlocking {
        enter()

        SignOut(signIn, SilentJournal)()
        assertEquals("kkm-1", signIn.state.value.kkm?.kkmId)
        assertNull(signIn.seat(), "пин ушедшего кассира остался годным для команд")

        SwitchKkm(signIn, SilentJournal)()
        assertNull(signIn.state.value.kkm)
    }

    @Test
    fun `перечитанная касса видна всем разделам`(): Unit = runBlocking {
        enter()
        core.on("getKkm") { CoreScene.kkm(name = "Касса у окна") }

        RefreshKkm(core.kassa(), signIn)()

        assertEquals("Касса у окна", signIn.state.value.kkm?.name)
    }

    @Test
    fun `без вошедшего кассира команда кассе не уходит`(): Unit = runBlocking {
        val answer = core.kassa().askSeated(signIn) { api, seat -> api.openShift(seat.kkmId, seat.pin) }

        assertIs<Answer.Failed>(answer)
        assertFalse("openShift" in core.calls)
    }

    @Test
    fun `пина нет в строке места за кассой`() {
        enter()

        assertFalse(CoreScene.PIN in signIn.seat().toString())
    }
}
