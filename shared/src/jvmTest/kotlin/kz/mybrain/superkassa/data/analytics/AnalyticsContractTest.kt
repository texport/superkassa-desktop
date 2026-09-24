package kz.mybrain.superkassa.data.analytics

import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsAnswer
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsTrouble
import kz.mybrain.superkassa.domain.analytics.model.PositionSource
import kz.mybrain.superkassa.domain.analytics.model.troubleOrNull
import kz.mybrain.superkassa.domain.analytics.model.valueOrNull
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetExpired
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Ответы аналитики кабинета доходят до предметной области приложения.
 *
 * Тела написаны по контракту, согласованному со стороной кабинета. Разбор
 * ответа проверяет модуль кабинета; здесь — что перевод в предметную область
 * ничего не теряет и что помехи названы своими именами.
 */
class AnalyticsContractTest {

    @Test
    fun `карта касс разбирается вместе с положением и счётчиками`() {
        val cabinet = CabinetReplies.always(
            """{"positionSource":"KKM_COORDINATES","placedCount":1,"withoutPositionCount":1,
               "placed":[{"cashRegisterId":"c1","kkmId":2000302,"registrationNumber":"KGD-2000302",
                 "internalName":"Касса у входа","retailPlaceId":"p1","retailPlaceName":"Магазин на Абая",
                 "address":"г. Алматы, пр. Абая, 10","status":"REGISTERED","blocked":false,
                 "shiftStatus":"OPEN","shiftNumber":12,"lastContactAt":"2026-09-19T08:14:00Z",
                 "position":{"source":"KKM_COORDINATES","latitude":43.238949,"longitude":76.889709,
                   "geoSource":"GNSS","rka":"1234567","cato":"750000000"}}],
               "withoutPosition":[{"cashRegisterId":"c2","kkmId":2000303,"status":"DRAFT","blocked":false}]}"""
        )
        val view = requireNotNull(runBlocking { cabinet.analytics.kkms(PositionSource.KkmCoordinates) }.valueOrNull())
        assertEquals(PositionSource.KkmCoordinates, view.source)
        assertEquals(1, view.placedCount)
        assertEquals(1, view.withoutPositionCount)
        val placed = view.placed.single()
        assertEquals("Касса у входа", placed.internalName)
        assertEquals(12L, placed.shiftNumber)
        assertEquals("GNSS", placed.position?.geoSource)
        assertTrue(placed.position?.degrees == true)
        assertEquals("c2", view.withoutPosition.single().cashRegisterId)
    }

    @Test
    fun `при адресе торговой точки координат нет, а адрес с РКА и САТО есть`() {
        val cabinet = CabinetReplies.always(
            """{"positionSource":"RETAIL_PLACE_ADDRESS","placedCount":1,"withoutPositionCount":0,
               "placed":[{"cashRegisterId":"c1","kkmId":2000302,"address":"г. Актобе, ул. Абилкайыр хана, 40",
                 "position":{"source":"RETAIL_PLACE_ADDRESS","rka":"7654321","cato":"151010000"}}],
               "withoutPosition":[]}"""
        )
        val answer = runBlocking { cabinet.analytics.kkms(PositionSource.RetailPlaceAddress) }
        val view = requireNotNull(answer.valueOrNull())
        val position = view.placed.single().position
        assertNull(position?.latitude)
        assertNull(position?.longitude)
        assertEquals("7654321", position?.rka)
        assertEquals("151010000", position?.cato)
        assertTrue(position?.degrees == false)
    }

    @Test
    fun `источник положения уходит строкой запроса`() {
        val cabinet = CabinetReplies.always("""{"placed":[],"withoutPosition":[]}""")
        runBlocking { cabinet.analytics.kkms(PositionSource.CabinetCoordinates) }
        assertEquals(listOf("/api/analytics/cash-registers/map?positionSource=CABINET_COORDINATES"), cabinet.asked)
    }

    @Test
    fun `адреса обмена разбираются со счётчиками и временем`() {
        val cabinet = CabinetReplies.always(
            """{"cashRegisterCount":2,"addressCount":3,"addresses":[
               {"cashRegisterId":"c1","kkmId":2000302,"registrationNumber":"KGD-2000302",
                "internalName":"Касса у входа","retailPlaceName":"Магазин на Абая","address":"212.154.10.7",
                "firstSeen":"2026-09-01T06:00:00Z","lastSeen":"2026-09-19T08:14:00Z"}]}"""
        )
        val view = requireNotNull(runBlocking { cabinet.analytics.exchange() }.valueOrNull())
        assertEquals(2, view.cashRegisterCount)
        assertEquals(3, view.addressCount)
        assertEquals("212.154.10.7", view.addresses.single().address)
    }

    @Test
    fun `у компании без обменов пустой список и нули, а не отказ`() {
        val cabinet = CabinetReplies.always("""{"cashRegisterCount":1,"addressCount":0,"addresses":[]}""")
        val view = requireNotNull(runBlocking { cabinet.analytics.exchange() }.valueOrNull())
        assertEquals(0, view.addressCount)
        assertTrue(view.addresses.isEmpty())
    }

    @Test
    fun `404 до выкладки кабинета — не отказ, а отсутствующий раздел`() {
        val cabinet = CabinetReplies.always("""{"title":"Not Found"}""", status = 404)
        val answer = runBlocking { cabinet.analytics.kkms(PositionSource.RetailPlaceAddress) }
        assertEquals(AnalyticsTrouble.NotDeployed, answer.troubleOrNull())
    }

    @Test
    fun `отказ по существу доходит своими словами, а молчание — отдельным случаем`() {
        val cabinet = CabinetReplies.always("""{"code":"FORBIDDEN","message":"no access"}""", status = 403)
        val refused = runBlocking { cabinet.analytics.exchange() }
        assertEquals(AnalyticsAnswer.Troubled(AnalyticsTrouble.Refused("no access")), refused)
        assertTrue(troubleOf(IllegalStateException("порт закрыт")) is AnalyticsTrouble.Unreachable)
    }

    @Test
    fun `кончившийся доступ владельца — просьба войти заново, а не молчание кабинета`() {
        assertEquals(AnalyticsTrouble.SignedOut, troubleOf(CabinetExpired()))
        val cabinet = CabinetReplies.always("""{"title":"Unauthorized"}""", status = 401)
        assertEquals(AnalyticsTrouble.SignedOut, runBlocking { cabinet.analytics.exchange() }.troubleOrNull())
    }
}
