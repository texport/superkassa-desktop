package kz.mybrain.superkassa.presentation.users.signin

import io.github.texport.superkassa.testing.api.kassa.ReadyKassa
import kz.mybrain.superkassa.kassa.CoreDesk
import kz.mybrain.superkassa.kassa.appKassa
import kz.mybrain.superkassa.presentation.common.message.Message
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Вход на настоящем ядре: список касс, пин кассира и администратора,
 * неверный пин, запертая касса, уход и смена кассы — действиями модели,
 * как их делает кассир на экране.
 */
class LoginCoreTest {
    private val desk = CoreDesk()
    private val model = loginModel(desk.app)

    @AfterTest
    fun close() = desk.close()

    @Test
    fun `из полусотни касс кассир выбирает нужную и входит в неё своим пином`() {
        val kassas = desk.bench.registerKassas(KKMS, appKassa(CoreDesk.ADMIN_PIN, CoreDesk.CASHIER_PIN))
        model.reload()
        assertEquals(KKMS, model.state.value.kkms.size, "список входа не весь")
        val wanted = kassas[WANTED]

        model.search("Касса ${WANTED + 1}")
        assertEquals(wanted.kkmId, model.state.value.chosen?.kkmId, "поиск по названию выбрал не ту кассу")
        model.typePin(CoreDesk.CASHIER_PIN)
        model.enter()

        val seated = desk.signIn.state.value
        assertEquals(wanted.kkmId, seated.kkm?.kkmId)
        assertTrue(seated.signedIn && !seated.isAdmin, "кассир вошёл не кассиром")
        assertEquals("", model.state.value.pin, "пин остался в поле после входа")
    }

    @Test
    fun `пин администратора даёт права администратора`() {
        enterWith(desk.register(), CoreDesk.ADMIN_PIN)

        assertTrue(desk.signIn.state.value.isAdmin)
    }

    @Test
    fun `неверный пин — отказ словами кассы, вход не начат, набранное не стёрто`() {
        enterWith(desk.register(), WRONG_PIN)

        val refusal = assertIs<Message.Refusal>(desk.said)
        assertTrue(refusal.text.isNotBlank())
        assertFalse(desk.signIn.state.value.signedIn)
        assertEquals(WRONG_PIN, model.state.value.pin, "кассиру придётся набирать пин заново")
        assertFalse(model.state.value.entering, "кнопка входа осталась занятой")
    }

    @Test
    fun `пятый неверный пин запирает кассу, и кассир видит, сколько ждать`() {
        val kassa = desk.register()
        repeat(LOCKING_TRIES) { enterWith(kassa, WRONG_PIN) }

        enterWith(kassa, CoreDesk.CASHIER_PIN)

        val refusal = assertIs<Message.Refusal>(desk.said)
        assertEquals("PIN_LOCKED", refusal.code)
        assertTrue("30" in refusal.text, "оставшееся время не названо: ${refusal.text}")
        assertFalse(desk.signIn.state.value.signedIn, "запертая касса пустила верным пином")
    }

    @Test
    fun `после блокировки верный пин снова пускает`() {
        val kassa = desk.register()
        repeat(LOCKING_TRIES) { enterWith(kassa, WRONG_PIN) }
        kassa.clock.move(LOCK + 1.seconds)

        enterWith(kassa, CoreDesk.CASHIER_PIN)

        assertTrue(desk.signIn.state.value.signedIn, "касса осталась запертой после срока: ${desk.saidText}")
        assertNull(desk.said as? Message.Refusal, "отказ прежнего пина остался на экране")
    }

    @Test
    fun `кассир ушёл — касса осталась выбранной, следующему нужен только пин`() {
        val kassa = desk.register()
        enterWith(kassa, CoreDesk.CASHIER_PIN)

        model.signOut()
        model.reload()

        assertFalse(desk.signIn.state.value.signedIn)
        assertEquals(kassa.kkmId, model.state.value.chosen?.kkmId, "после ухода кассу снова ищут в списке")
        model.typePin(CoreDesk.ADMIN_PIN)
        model.enter()
        assertTrue(desk.signIn.state.value.isAdmin, "следующий не вошёл на ту же кассу")
    }

    @Test
    fun `смена кассы — вход начинается заново, и пин прежней кассы в другую не пускает`() {
        val first = desk.register(name = "Первая")
        val second = desk.bench.registerKassa(appKassa("5930", "6147", name = "Вторая"))
        enterWith(first, CoreDesk.CASHIER_PIN)

        desk.signIn.switchKkm()
        model.reload()
        model.pick(model.state.value.kkms.single { it.kkmId == second.kkmId })
        model.typePin(CoreDesk.CASHIER_PIN)
        model.enter()

        assertFalse(desk.signIn.state.value.signedIn, "пин первой кассы открыл вторую")
        model.typePin(second.cashierPin)
        model.enter()
        assertEquals(second.kkmId, desk.signIn.state.value.kkm?.kkmId)
    }

    /** Выбирает кассу мышью, набирает пин и жмёт «Войти». */
    private fun enterWith(kassa: ReadyKassa, pin: String) {
        model.reload()
        model.pick(model.state.value.kkms.single { it.kkmId == kassa.kkmId })
        model.typePin(pin)
        model.enter()
    }

    private companion object {
        const val KKMS = 50
        const val WANTED = 36
        const val WRONG_PIN = "0000"
        const val LOCKING_TRIES = 5
        val LOCK = 30.seconds
    }
}
