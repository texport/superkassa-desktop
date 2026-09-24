package kz.mybrain.superkassa.integrations.bfdcabinet

import io.ktor.client.engine.mock.MockEngine
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetFake.Reply
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Сеанс владельца: вход по подписи, доступ в каждом обращении и четыре вида
 * неудачи, которые приложению нельзя путать.
 */
class CabinetSessionTest {

    @Test
    fun signInSignsTheChallengeAndKeepsAccess() = runTest {
        val fake = CabinetFake.always("""{"id":"c","bin":"230140000000","name":"ТОО Азик и Ко"}""")
        val cabinet = fake.cabinet()

        assertEquals(listOf("cms-of-cGF5bG9hZA=="), fake.signed)
        assertEquals("900101300000", cabinet.account.owner.value?.user?.iin)
        cabinet.company.company()
        assertEquals("Bearer access-1", fake.asked.single().headers[HttpHeaders.Authorization])
    }

    /** 401 при выданном доступе — конец сеанса: владельца возвращают ко входу. */
    @Test
    fun unauthorizedAfterSignInIsExpiry() = runTest {
        val expired = CabinetFake.always("""{"code":"UNAUTHORIZED","message":"Сессия истекла"}""", status = 401)
        val cabinet = expired.cabinet()

        assertFailsWith<CabinetExpired> { cabinet.company.company() }
        assertNull(cabinet.account.owner.value, "имя вошедшего осталось после конца сеанса")
    }

    /** 401 при входе — отказ по подписи, а не конец сеанса: повтор тем же ключом не поможет. */
    @Test
    fun unauthorizedAtSignInIsRefusal() = runTest {
        val refused = Reply("""{"code":"EDS_INVALID","detail":"Сертификат просрочен"}""", status = 401)
        val cabinet = CabinetFake(login = refused) { null }.unsigned()
        val refusal = assertFailsWith<CabinetRefusal> { cabinet.account.signIn() }

        assertEquals("EDS_INVALID", refusal.code)
        assertEquals("Сертификат просрочен", refusal.text)
        assertNull(cabinet.account.owner.value)
    }

    @Test
    fun callWithoutSignInIsExpiry() = runTest {
        val fake = CabinetFake.always("{}")

        assertFailsWith<CabinetExpired> { fake.unsigned().places.page() }
        assertEquals(0, fake.asked.size, "без доступа запрос ушёл в кабинет")
    }

    @Test
    fun silentCabinetIsUnreachable() = runTest {
        val silent = BfdCabinet(signer = { "cms" }, engine = MockEngine { throw IOException("Connection refused") })
        val failure = assertFailsWith<CabinetUnreachable> { silent.account.signIn() }

        assertNotNull(failure.cause)
    }

    /** Ответ успехом, который не читается, — разошедшийся договор, а не недоступность. */
    @Test
    fun unreadableSuccessIsUnreadable() = runTest {
        val cabinet = CabinetFake.always("""{"page":"первая","items":"нет"}""").cabinet()
        val failure = assertFailsWith<CabinetUnreadable> { cabinet.places.page() }

        assertEquals("/api/retail-places?page=0&size=50", failure.path)
    }

    @Test
    fun refusalWithoutCodeIsNamedByStatus() = runTest {
        val cabinet = CabinetFake.always("<html>gateway</html>", status = 502).cabinet()
        val refusal = assertFailsWith<CabinetRefusal> { cabinet.company.company() }

        assertEquals("HTTP_502", refusal.code)
        assertEquals("<html>gateway</html>", refusal.text)
    }

    /** Выход забывает доступ и тогда, когда кабинет выход не принял. */
    @Test
    fun signOutForgetsAccessEvenWhenCabinetFails() = runTest {
        val cabinet = CabinetFake.always("{}", status = 500).cabinet()

        assertFailsWith<CabinetRefusal> { cabinet.account.signOut() }
        assertNull(cabinet.account.owner.value)
        assertFailsWith<CabinetExpired> { cabinet.company.company() }
    }
}
