package kz.mybrain.superkassa.domain.setup.usecase

import io.github.texport.superkassa.core.domain.api.exception.NotFoundException
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmInitSimpleRequest
import io.github.texport.superkassa.core.string.api.TrilingualMessage
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.setup.model.EnrollOutcome
import kz.mybrain.superkassa.domain.setup.model.EnrollmentPlan
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.SilentJournal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Заведение кассы: касса считается заведённой, только когда она читается.
 *
 * Дефект кассы процесса: при молчащей БФД заведение отвечает успехом
 * и сведениями о кассе, которой в базе нет.
 */
class EnrollKkmTest {
    private val core = FakeCore()
    private val enroll = EnrollKkm(core.kassa(), SilentJournal)
    private val plan = EnrollmentPlan(contour = "DEV", systemId = "5000021", adminPin = "4821", name = "Касса у входа")

    @Test
    fun `заведённая и читаемая касса названа и объявлена`(): Unit = runBlocking {
        var request: KkmInitSimpleRequest? = null
        var named: List<Any?> = emptyList()
        core.on("initKkmSimple") { args ->
            request = args.single() as KkmInitSimpleRequest
            CoreScene.kkm(id = "kkm-9")
        }
        core.on("getKkm") { CoreScene.kkm(id = "kkm-9") }
        core.on("updateKkmName") { args -> CoreScene.kkm(id = "kkm-9", name = "Касса у входа").also { named = args } }

        val outcome = enroll(plan) { "3735928559" }

        assertEquals("Касса у входа", assertIs<EnrollOutcome.Enrolled>(outcome).kkm.name)
        assertEquals("4821", request?.adminPin, "пин администратора не тот, что набрал владелец")
        assertEquals("BFD", request?.ofdId)
        assertEquals(listOf<Any?>("kkm-9", "4821", "Касса у входа"), named)
    }

    @Test
    fun `касса, которой нет в базе, заведённой не считается`(): Unit = runBlocking {
        core.on("initKkmSimple") { CoreScene.kkm(id = "kkm-ghost") }
        core.on("getKkm") { throw NotFoundException(TrilingualMessage("нет", "жоқ", "none"), "KKM_NOT_FOUND") }

        assertEquals(EnrollOutcome.NotStored, enroll(plan) { "1" })
        assertTrue("updateKkmName" !in core.calls, "названа касса, которой нет")
    }

    @Test
    fun `без идентификатора или токена кассу не заводят`(): Unit = runBlocking {
        assertEquals(EnrollOutcome.Skipped, enroll(plan.copy(systemId = null)) { "1" })
        assertEquals(EnrollOutcome.Skipped, enroll(plan) { null })
        assertTrue(core.calls.isEmpty(), "касса спрошена: ${core.calls}")
    }

    @Test
    fun `отказ кассы передаётся её словами`(): Unit = runBlocking {
        core.refuse("initKkmSimple", "KKM_SYSTEM_ID_EXISTS", ru = "Касса уже есть")

        val refused = assertIs<EnrollOutcome.Refused>(enroll(plan) { "1" })
        assertEquals("KKM_SYSTEM_ID_EXISTS", assertIs<Answer.Refused>(refused.answer).code)
    }
}
