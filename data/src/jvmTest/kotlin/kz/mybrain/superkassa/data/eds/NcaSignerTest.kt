package kz.mybrain.superkassa.data.eds

import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.cabinet.model.EdsProblem
import kz.mybrain.superkassa.domain.cabinet.model.EdsRefusal
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kz.mybrain.superkassa.integrations.ncalayer.NcaLayer
import kz.mybrain.superkassa.integrations.ncalayer.NcaReason
import kz.mybrain.superkassa.integrations.ncalayer.NcaRefusal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

/**
 * Отказ NCALayer — отказом подписи предметной области.
 *
 * Протокол проверяет модуль `ncalayer`; здесь — что владелец прочтёт верное:
 * «запустите NCALayer» только про неотвечающий, а про молчание, закрытое
 * окно и отказ — их собственными словами.
 */
class NcaSignerTest {

    private fun layer(answer: suspend (String) -> String) = object : NcaLayer {
        override suspend fun sign(payload: String): String = answer(payload)
    }

    private fun refused(reason: NcaReason, detail: String = reason.name): EdsRefusal {
        val cause = NcaRefusal(reason, detail)
        val signer = NcaSigner(layer { throw cause })
        return assertFailsWith<EdsRefusal> { runBlocking { signer.sign("cGF5bG9hZA==") } }.also {
            assertSame(cause, it.cause, "причина отказа потеряна: по ней разбирают журнал")
        }
    }

    @Test
    fun `молчание и недоступность названы по-разному`() {
        val absent = refused(NcaReason.Unreachable)
        val silent = refused(NcaReason.Silent)

        assertEquals(EdsProblem.Unreachable, absent.problem)
        assertEquals(Signer.NO_HANDSHAKE, absent.detail)
        assertEquals(EdsProblem.Declined, silent.problem, "про запущенный NCALayer сказали бы «запустите его»")
        assertEquals(Signer.NO_ANSWER, silent.detail)
    }

    @Test
    fun `закрытое окно названо закрытым окном`() {
        val closed = refused(NcaReason.WindowClosed)

        assertEquals(EdsProblem.Declined, closed.problem)
        assertEquals(Signer.WINDOW_CLOSED, closed.detail)
    }

    /** Отказ NCALayer доходит его словами: по ним видно, что владелец отменил сам. */
    @Test
    fun `подробности отказа отделены от причины`() {
        val declined = refused(NcaReason.Declined, "action.canceled")

        assertEquals(EdsProblem.Declined, declined.problem)
        assertEquals("action.canceled", declined.detail)
        assertEquals(true, declined.cancelled)
    }

    @Test
    fun `подпись отдаётся как есть`() {
        val signer = NcaSigner(layer { payload -> "cms-of-$payload" })

        assertEquals("cms-of-cGF5bG9hZA==", runBlocking { signer.sign("cGF5bG9hZA==") })
    }
}
