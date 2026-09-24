package kz.mybrain.superkassa.domain.setup.usecase

import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.setup.port.FakeSetupCabinet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Подача заявления о постановке на учёт: подготовка, подпись владельца, отправка. */
class SubmitRegistrationTest {
    private val cabinet = FakeSetupCabinet()
    private val signing = mutableListOf<Boolean>()

    @Test
    fun `подписанное уходит под действием, которое назвал кабинет`(): Unit = runBlocking {
        SubmitRegistration(cabinet)("r-1") { signing += it }

        val sent = "send r-1 ${FakeSetupCabinet.ACTION} signed-${FakeSetupCabinet.PAYLOAD}"
        assertEquals(listOf("prepare r-1", sent), cabinet.asked)
        assertEquals(listOf(true, false), signing, "срок подписи не показан или не снят")
    }

    /** Владелец не подписал: в КГД не уходит ничего, а срок подписи снят. */
    @Test
    fun `без подписи заявление не отправляется`(): Unit = runBlocking {
        cabinet.signer = { error("declined") }

        assertFailsWith<IllegalStateException> { SubmitRegistration(cabinet)("r-1") { signing += it } }

        assertTrue(cabinet.asked.none { it.startsWith("send") }, "неподписанное отправлено: ${cabinet.asked}")
        assertEquals(listOf(true, false), signing)
    }
}
