package kz.mybrain.superkassa.domain.cabinet.usecase.applications

import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.cabinet.model.ApplicationStage
import kz.mybrain.superkassa.domain.cabinet.model.CabinetApplication
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRefusal
import kz.mybrain.superkassa.domain.cabinet.model.EdsProblem
import kz.mybrain.superkassa.domain.cabinet.model.EdsRefusal
import kz.mybrain.superkassa.domain.cabinet.model.documents.ApplicationPrepared
import kz.mybrain.superkassa.domain.cabinet.model.documents.ApplicationSent
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegistrationAction
import kz.mybrain.superkassa.domain.cabinet.model.documents.SignRequest
import kz.mybrain.superkassa.domain.cabinet.port.CabinetApplications
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Подача заявления: подготовка, подпись владельца, отправка — по порядку.
 *
 * Между подготовкой и отправкой стоит владелец с ключом. Не подписал —
 * в кабинет ничего не уходит: заявление остаётся черновиком.
 */
class SubmitApplicationTest {
    private val sent = mutableListOf<SignRequest>()
    private val stages = mutableListOf<ApplicationStage>()

    private val cabinet = object : CabinetApplications {
        var refusal: CabinetRefusal? = null

        override suspend fun prepare(application: CabinetApplication): ApplicationPrepared {
            refusal?.let { throw it }
            return ApplicationPrepared(actionId = "a-1", actionType = "REGISTRATION", payloadToSign = "cGF5bG9hZA==")
        }

        override suspend fun send(application: CabinetApplication, sign: SignRequest): ApplicationSent {
            sent += sign
            return ApplicationSent(actionId = sign.actionId, actionStatus = "SENT")
        }

        override suspend fun actions(registerId: String): List<RegistrationAction> = emptyList()
    }

    @Test
    fun `подписанное уходит с подписью владельца`(): Unit = runBlocking {
        val submit = SubmitApplication(cabinet, signing { "MIIC-$it" })

        val result = submit(CabinetApplication.Registration("r-1")) { stages += it }

        assertEquals("SENT", result.actionStatus)
        assertEquals(listOf(SignRequest("a-1", "MIIC-cGF5bG9hZA==")), sent)
        assertEquals(listOf(ApplicationStage.Preparing, ApplicationStage.Signing, ApplicationStage.Sending), stages)
    }

    @Test
    fun `без подписи в кабинет ничего не уходит`(): Unit = runBlocking {
        val submit = SubmitApplication(cabinet, signing { throw EdsRefusal(EdsProblem.Declined, "cancelled") })

        assertFailsWith<EdsRefusal> { submit(CabinetApplication.Registration("r-1")) { stages += it } }

        assertTrue(sent.isEmpty(), "неподписанное ушло в кабинет")
        assertEquals(ApplicationStage.Signing, stages.last())
    }

    @Test
    fun `отказ кабинета при подготовке не зовёт владельца подписывать`(): Unit = runBlocking {
        var asked = false
        cabinet.refusal = CabinetRefusal("REGISTER_NOT_DRAFT", "Касса уже на учёте", httpStatus = 409)
        val submit = SubmitApplication(
            cabinet,
            signing {
                asked = true
                it
            }
        )

        val refusal = assertFailsWith<CabinetRefusal> { submit(CabinetApplication.Registration("r-1")) {} }

        assertEquals("REGISTER_NOT_DRAFT", refusal.code)
        assertTrue(!asked && sent.isEmpty())
    }

    private fun signing(answer: (String) -> String) = object : Signer {
        override suspend fun sign(payload: String): String = answer(payload)
    }
}
