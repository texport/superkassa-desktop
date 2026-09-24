package kz.mybrain.superkassa.domain.setup.usecase

import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import io.github.texport.superkassa.testing.api.bfd.FakeBfd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.setup.model.EnrollOutcome
import kz.mybrain.superkassa.domain.setup.model.EnrollmentPlan
import kz.mybrain.superkassa.kassa.SilentJournal
import kz.mybrain.superkassa.kassa.appBench
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Заведение кассы мастером — на настоящем ядре и тестовом БФД.
 *
 * Пинов по умолчанию у кассы нет: администратор новой кассы входит ровно
 * тем пином, что набрал владелец, а без пина или с пином не той длины
 * касса не заводится и называет причину своим кодом.
 */
class EnrollKkmCoreTest {
    private val directory: File = createTempDirectory("kassa-enroll-").toFile()
    private val bench = appBench(directory)
    private val kassa = EmbeddedKassa(bench.api, Dispatchers.Unconfined)
    private val enroll = EnrollKkm(kassa, SilentJournal)

    @AfterTest
    fun close() {
        bench.close()
        directory.deleteRecursively()
    }

    private fun plan(adminPin: String) =
        EnrollmentPlan(contour = "DEV", systemId = "100001", adminPin = adminPin, name = "Касса у входа")

    private fun enrolled(adminPin: String): EnrollOutcome =
        runBlocking { enroll(plan(adminPin)) { FakeBfd.FIRST_TOKEN.toString() } }

    @Test
    fun `касса заведена, названа и открывается пином, набранным владельцем`(): Unit = runBlocking {
        val kkm = assertIs<EnrollOutcome.Enrolled>(enrolled("7391")).kkm

        assertEquals("Касса у входа", kkm.name)
        val role = when (val admin = kassa.ask { it.authenticate(kkm.kkmId, "7391") }) {
            is Answer.Done -> admin.value.role
            else -> null
        }
        assertEquals(UserRole.ADMIN, role, "пин, набранный владельцем, не пустил администратора")
    }

    @Test
    fun `пин из одинаковых цифр — обычный пин администратора`(): Unit = runBlocking {
        val kkm = assertIs<EnrollOutcome.Enrolled>(enrolled("0000")).kkm

        assertIs<Answer.Done<*>>(kassa.ask { it.authenticate(kkm.kkmId, "0000") })
    }

    @Test
    fun `без пина администратора касса не заводится`() {
        val refused = assertIs<Answer.Refused>(assertIs<EnrollOutcome.Refused>(enrolled("")).answer)

        assertEquals("KKM_ADMIN_PIN_REQUIRED", refused.code)
        assertTrue(refused.ru.isNotBlank() && refused.kk.isNotBlank() && refused.en.isNotBlank())
    }

    @Test
    fun `пин администратора не той длины касса не принимает`() {
        val refused = assertIs<Answer.Refused>(assertIs<EnrollOutcome.Refused>(enrolled("482")).answer)

        assertEquals("USER_PIN_LENGTH", refused.code)
    }
}
