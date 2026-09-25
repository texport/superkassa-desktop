package kz.mybrain.superkassa.domain.setup.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Шаги мастера подключения: какие они на каждом пути, с какого продолжить
 * брошенное и на каком можно стоять.
 */
class SetupRouteTest {
    private val viaCabinet = SetupRoute(SetupWay.ViaCabinet, choosing = true)
    private val byHand = SetupRoute(SetupWay.ByHand, choosing = true)

    @Test
    fun `через кабинет — пять шагов, вручную — четыре, без кабинета — три`() {
        val cabinet = listOf(
            SetupStep.Way,
            SetupStep.Factory,
            SetupStep.Cabinet,
            SetupStep.Application,
            SetupStep.Admin
        )
        assertEquals(cabinet, viaCabinet.steps)
        assertEquals(listOf(SetupStep.Way, SetupStep.Factory, SetupStep.Credentials, SetupStep.Admin), byHand.steps)
        val alone = SetupRoute(SetupWay.ByHand, choosing = false)
        assertEquals(listOf(SetupStep.Factory, SetupStep.Credentials, SetupStep.Admin), alone.steps)
        assertEquals(SetupStep.Factory, alone.first)
    }

    @Test
    fun `номер шага и следующий шаг`() {
        assertEquals(4, viaCabinet.number(SetupStep.Application))
        assertEquals(SetupStep.Admin, viaCabinet.after(SetupStep.Application))
        assertNull(viaCabinet.after(SetupStep.Admin))
    }

    @Test
    fun `брошенный мастер продолжается с первого непройденного шага`() {
        val chosen = KkmSetupDraft(way = SetupWay.ViaCabinet)
        assertEquals(SetupStep.Way, viaCabinet.resume(KkmSetupDraft()))
        assertEquals(SetupStep.Factory, viaCabinet.resume(chosen))
        assertEquals(SetupStep.Cabinet, viaCabinet.resume(chosen.copy(factoryNumber = FACTORY)))
        val registered = chosen.copy(factoryNumber = FACTORY, cabinetRegisterId = REGISTER)
        assertEquals(SetupStep.Application, viaCabinet.resume(registered))
        val laid = listOf(SetupStep.Factory, SetupStep.Cabinet, SetupStep.Application)
        assertEquals(laid, viaCabinet.toResume(registered))
    }

    @Test
    fun `ручной путь продолжается с идентификатора и токена — их мастер не помнит`() {
        val draft = KkmSetupDraft(factoryNumber = FACTORY, way = SetupWay.ByHand)
        assertEquals(SetupStep.Credentials, byHand.resume(draft))
        assertEquals(emptyList(), byHand.toResume(KkmSetupDraft()))
    }

    @Test
    fun `на шаге без пройденного предыдущего не стоят`() {
        val fresh = KkmSetupDraft()
        assertFalse(viaCabinet.opens(SetupStep.Cabinet, fresh))
        assertFalse(viaCabinet.opens(SetupStep.Application, fresh.copy(factoryNumber = FACTORY)))
        assertFalse(viaCabinet.opens(SetupStep.Admin, fresh.copy(factoryNumber = FACTORY)))
        assertFalse(viaCabinet.opens(SetupStep.Credentials, fresh), "шаг чужого пути открылся")
        assertTrue(byHand.opens(SetupStep.Admin, fresh))
        assertTrue(viaCabinet.opens(SetupStep.Admin, fresh.copy(cabinetRegisterId = REGISTER)))
    }

    @Test
    fun `путь продолженного мастера — выбранный, а без кабинета — только вручную`() {
        val chosen = KkmSetupDraft(way = SetupWay.ByHand)
        assertEquals(SetupWay.ByHand, SetupRoute.wayOf(chosen, withCabinet = true))
        assertEquals(SetupWay.ViaCabinet, SetupRoute.wayOf(KkmSetupDraft(), withCabinet = true))
        val cabinetChosen = KkmSetupDraft(way = SetupWay.ViaCabinet)
        assertEquals(SetupWay.ByHand, SetupRoute.wayOf(cabinetChosen, withCabinet = false))
    }

    private companion object {
        const val FACTORY = "KZT26E2C509A200"
        const val REGISTER = "r-1"
    }
}
