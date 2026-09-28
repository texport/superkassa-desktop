package kz.mybrain.superkassa.domain.cabinet.usecase.register

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmInitDirectRequest
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.CompanyProfile
import kz.mybrain.superkassa.domain.cabinet.model.Oked
import kz.mybrain.superkassa.domain.cabinet.port.CabinetCompanies
import kz.mybrain.superkassa.domain.cabinet.unwired
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

/**
 * Касса кабинета заводится на этой машине пином, который задал владелец.
 *
 * Своего пина приложение не подставляет. Касса при недоступном ОФД
 * отвечала на заведение успехом и кассой, которой в её базе нет, — поэтому
 * заведённое перечитывается, и отказ перечитывания есть итог заведения.
 */
class EnrollKkmHereTest {
    private val core = FakeCore()
    private var profile: CompanyProfile? = CompanyProfile("c-1", "000000000000", "ТОО «Пример»", PRIMARY)
    private val company = object : CabinetCompanies by unwired() {
        override suspend fun company(): CompanyProfile = profile ?: error("кабинет не ответил")
    }
    private val enroll = EnrollKkmHere(core.kassa(), company)

    @Test
    fun `касса заводится пином владельца и получает имя из кабинета`(): Unit = runBlocking {
        var init: List<Any?> = emptyList()
        var named: List<Any?> = emptyList()
        core.on("initKkm") { args -> CoreScene.kkm(name = null).also { init = args } }
        core.on("getKkm") { CoreScene.kkm(name = null) }
        core.on("updateKkmName") { args -> CoreScene.kkm().also { named = args } }

        val enrolled = enroll(REGISTER, "BFD", "TEST", ADMIN_PIN, TOKEN)

        assertIs<Answer.Done<*>>(enrolled)
        val request = init.single() as KkmInitDirectRequest
        assertEquals(ADMIN_PIN, request.adminPin, "касса заведена не тем пином, что задал владелец")
        assertEquals("5000021", request.ofdSystemId)
        assertEquals(TOKEN, request.ofdToken)
        assertEquals("47111", request.oked, "касса заведена не с основным ОКЭДом компании")
        assertEquals(listOf("kkm-1", ADMIN_PIN, "Касса у входа"), named)
    }

    /**
     * БФД о кассе на учёте присылал пустой ОКЭД, и касса отказывала
     * «ОКЭД обязателен». ОКЭД берётся у компании: основной, иначе первый.
     */
    @Test
    fun `без основного вида деятельности касса заводится с первым`(): Unit = runBlocking {
        profile = profile?.copy(okeds = listOf(Oked("62010"), Oked("47111")))
        var init: List<Any?> = emptyList()
        core.on("initKkm") { args -> CoreScene.kkm().also { init = args } }
        core.on("getKkm") { CoreScene.kkm() }
        core.on("updateKkmName") { CoreScene.kkm() }

        enroll(REGISTER, "BFD", "TEST", ADMIN_PIN, TOKEN)

        assertEquals("62010", (init.single() as KkmInitDirectRequest).oked)
    }

    @Test
    fun `компания не прочиталась — касса заводится с ОКЭДом от БФД`(): Unit = runBlocking {
        profile = null
        var init: List<Any?> = emptyList()
        core.on("initKkm") { args -> CoreScene.kkm().also { init = args } }
        core.on("getKkm") { CoreScene.kkm() }
        core.on("updateKkmName") { CoreScene.kkm() }

        assertIs<Answer.Done<*>>(enroll(REGISTER, "BFD", "TEST", ADMIN_PIN, TOKEN))
        assertEquals(null, (init.single() as KkmInitDirectRequest).oked)
    }

    @Test
    fun `касса, которой после заведения нет, заведённой не считается`(): Unit = runBlocking {
        core.on("initKkm") { CoreScene.kkm() }
        core.refuse("getKkm", "KKM_NOT_FOUND", ru = "Касса не найдена")

        val enrolled = enroll(REGISTER, "BFD", "TEST", ADMIN_PIN, TOKEN)

        assertEquals("KKM_NOT_FOUND", assertIs<Answer.Refused>(enrolled).code)
        assertFalse("updateKkmName" in core.calls, "имя писалось в кассу, которой нет")
    }

    @Test
    fun `отказ заведения доходит кодом кассы`(): Unit = runBlocking {
        core.refuse("initKkm", "OFD_UNAVAILABLE", ru = "ОФД недоступен")

        val enrolled = enroll(REGISTER, "BFD", "TEST", ADMIN_PIN, TOKEN)

        assertEquals("OFD_UNAVAILABLE", assertIs<Answer.Refused>(enrolled).code)
        assertFalse("getKkm" in core.calls)
    }

    private companion object {
        val PRIMARY = listOf(Oked("62010"), Oked("47111", primary = true))
        const val ADMIN_PIN = "4826"
        const val TOKEN = "3000000001"
        val REGISTER = CabinetRegister(
            id = "r-1",
            kkmId = 5_000_021,
            internalName = "Касса у входа",
            status = "REGISTERED",
            registrationNumber = "000000200042",
            factoryNumber = "SN-ECC-172758",
            manufactureYear = 2026
        )
    }
}
