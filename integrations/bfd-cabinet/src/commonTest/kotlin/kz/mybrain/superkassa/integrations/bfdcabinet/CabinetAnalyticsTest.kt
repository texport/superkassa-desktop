package kz.mybrain.superkassa.integrations.bfdcabinet

import kotlinx.coroutines.test.runTest
import kz.mybrain.superkassa.integrations.bfdcabinet.analytics.PositionSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Карта касс и адреса обмена.
 *
 * Тела написаны по контракту, согласованному со стороной кабинета: пока
 * ручки не выложены, эта проверка — единственное место, где контракт
 * проверяется целиком.
 */
class CabinetAnalyticsTest {

    @Test
    fun mapIsReadWithPositionAndCounters() = runTest {
        val view = CabinetFake.always(
            """{"positionSource":"KKM_COORDINATES","placedCount":1,"withoutPositionCount":1,
               "placed":[{"cashRegisterId":"c1","kkmId":2000302,"registrationNumber":"000000010001",
                 "internalName":"Касса у входа","retailPlaceId":"p1","retailPlaceName":"Магазин на Абая",
                 "address":"г. Алматы, пр. Абая, 10","status":"REGISTERED","blocked":false,
                 "shiftStatus":"OPEN","shiftNumber":12,"lastContactAt":"2026-09-19T08:14:00Z",
                 "position":{"source":"KKM_COORDINATES","latitude":43.238949,"longitude":76.889709,
                   "geoSource":"GNSS","rka":"1234567","cato":"750000000"}}],
               "withoutPosition":[{"cashRegisterId":"c2","kkmId":2000303,"status":"DRAFT","blocked":false}]}"""
        ).cabinet().analytics.map(PositionSource.KkmCoordinates)
        val placed = view.placed.single()

        assertEquals(PositionSource.KkmCoordinates, view.source)
        assertEquals("Касса у входа", placed.internalName)
        assertEquals(12L, placed.shiftNumber)
        assertEquals("GNSS", placed.position?.geoSource)
        assertEquals(43.238949, placed.position?.latitude)
        assertEquals("c2", view.withoutPosition.single().cashRegisterId)
    }

    /** При адресе торговой точки координат нет: точку по адресу ставит приложение. */
    @Test
    fun retailAddressSourceHasNoCoordinates() = runTest {
        val position = CabinetFake.always(
            """{"positionSource":"RETAIL_PLACE_ADDRESS","placed":[{"cashRegisterId":"c1","kkmId":2000302,
               "address":"г. Актобе, ул. Абилкайыр хана, 40",
               "position":{"source":"RETAIL_PLACE_ADDRESS","rka":"7654321","cato":"151010000"}}],
               "withoutPosition":[]}"""
        ).cabinet().analytics.map(PositionSource.RetailPlaceAddress).placed.single().position

        assertNull(position?.latitude)
        assertEquals("7654321", position?.rka)
        assertEquals("151010000", position?.cato)
    }

    @Test
    fun mapFilterGoesInQueryAndUnsetIsNotWritten() = runTest {
        val fake = CabinetFake.always("""{"placed":[],"withoutPosition":[]}""")
        fake.cabinet().analytics.map(PositionSource.CabinetCoordinates, retailPlaceId = "p 1")

        val expected = "/api/analytics/cash-registers/map?positionSource=CABINET_COORDINATES&retailPlaceId=p+1"
        assertEquals(expected, fake.asked.single().target())
    }

    @Test
    fun exchangeAddressesAreReadForCompanyAndRegister() = runTest {
        val fake = CabinetFake.always(
            """{"cashRegisterCount":2,"addressCount":3,"addresses":[
               {"cashRegisterId":"c1","kkmId":2000302,"registrationNumber":"000000010001",
                "internalName":"Касса у входа","retailPlaceName":"Магазин на Абая","address":"212.154.10.7",
                "firstSeen":"2026-09-01T06:00:00Z","lastSeen":"2026-09-19T08:14:00Z"}]}"""
        )
        val cabinet = fake.cabinet()
        val all = cabinet.analytics.exchangeAddresses()
        cabinet.analytics.exchangeAddresses("c1")

        assertEquals(3, all.addressCount)
        assertEquals("212.154.10.7", all.addresses.single().address)
        assertEquals("/api/analytics/cash-registers/c1/addresses", fake.asked.last().url.encodedPath)
    }

    /** До выкладки кабинет отвечает 404 — это «раздела нет», и приложение отличает его по состоянию. */
    @Test
    fun notDeployedSectionIsRefusalWith404() = runTest {
        val cabinet = CabinetFake.always("""{"title":"Not Found"}""", status = 404).cabinet()
        val refusal = assertFailsWith<CabinetRefusal> { cabinet.analytics.map(PositionSource.RetailPlaceAddress) }

        assertEquals(404, refusal.httpStatus)
        assertEquals("Not Found", refusal.text)
    }

    @Test
    fun forbiddenIsRefusalInCabinetWords() = runTest {
        val cabinet = CabinetFake.always("""{"code":"FORBIDDEN","message":"no access"}""", status = 403).cabinet()
        val refusal = assertFailsWith<CabinetRefusal> { cabinet.analytics.exchangeAddresses() }

        assertEquals("no access", refusal.text)
        assertTrue(refusal.httpStatus == 403)
    }
}
