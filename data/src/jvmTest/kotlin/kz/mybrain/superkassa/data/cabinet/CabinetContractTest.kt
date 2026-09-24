package kz.mybrain.superkassa.data.cabinet

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.CabinetWire
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRefusal
import kz.mybrain.superkassa.domain.cabinet.port.CabinetPorts
import kz.mybrain.superkassa.jsonHttp
import kz.mybrain.superkassa.signedPorts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Ответы кабинета `bfd-cabinet.ecc.kz`, снятые 2026-09-17, доходят через
 * порты кабинета до моделей предметной области. Тела взяты из живых ответов
 * без правок: если кабинет поменяет схему, этот тест покажет, что именно
 * перестало читаться. Разбор DTO проверяет модуль кабинета; здесь — что
 * прочитанное не теряется при переводе в модели приложения.
 */
class CabinetContractTest {

    private var seenHeaders: Map<String, String?> = emptyMap()

    private fun cabinetReturning(body: String, status: HttpStatusCode = HttpStatusCode.OK): CabinetPorts {
        val engine = MockEngine { request ->
            seenHeaders = mapOf(
                HttpHeaders.Authorization to request.headers[HttpHeaders.Authorization],
                "Idempotency-Key" to request.headers["Idempotency-Key"]
            )
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return CabinetWire(http = jsonHttp(engine)).signedPorts()
    }

    @Test
    fun `карточка и строка списка дают модель и точку одинаково`() {
        val card = runBlocking {
            cabinetReturning(
                """{"id":"07aeaff1","kkmId":5000001,"internalName":"Касса демо","status":"DRAFT","registrationNumber":null,
                   "factoryNumber":"SN-ECC-172758","manufactureYear":2026,
                   "model":{"modelCode":"0x0065000086cb","name":"«ПОРТ FPG-350 ФKZ»"},
                   "retailPlace":{"id":"db5b84e9","name":"Магазин на Абая"},
                   "lastRegistrationAction":{"actionId":"074426f8","actionType":"REGISTRATION","status":"DRAFT",
                   "stateSyncStatus":"NOT_REQUIRED","sentAt":null,"completedAt":null},
                   "registrationCardAvailable":false}"""
            ).registers.one("07aeaff1")
        }
        val listed = runBlocking {
            cabinetReturning(
                """{"page":0,"size":5,"totalElements":1,"items":[{"id":"07aeaff1","kkmId":5000001,"internalName":"Касса демо",
                   "status":"DRAFT","registrationNumber":null,"factoryNumber":"SN-ECC-172758","modelName":"«ПОРТ FPG-350 ФKZ»",
                   "retailPlaceId":"db5b84e9","retailPlaceName":"Магазин на Абая"}]}"""
            ).registers.all().single()
        }
        assertEquals("«ПОРТ FPG-350 ФKZ»", card.model?.name)
        assertEquals("«ПОРТ FPG-350 ФKZ»", listed.model?.name)
        assertEquals("db5b84e9", card.retailPlace?.id)
        assertEquals("db5b84e9", listed.retailPlace?.id)
        assertEquals("REGISTRATION", card.lastRegistrationAction?.type)
    }

    @Test
    fun `состояние без кассы у сервера читается как не найденное`() {
        val state = runBlocking {
            cabinetReturning(
                """{"cashRegisterId":"07aeaff1","businessStatus":"DRAFT","stateSyncStatus":"NOT_REQUIRED",
                   "technicalState":{"found":false,"active":false,"inactiveReason":"NOT_REGISTERED","shiftStatus":"UNKNOWN",
                   "shiftNumber":null,"validationMask":null,"lastContactAt":null}}"""
            ).registers.state("07aeaff1")
        }
        assertEquals(false, state.technicalState?.found)
        assertNull(state.technicalState?.status)
    }

    @Test
    fun `журнал действий и смены читаются по именам кабинета`() {
        val action = runBlocking {
            cabinetReturning(
                """{"page":0,"size":5,"totalElements":1,"items":[{"actionId":"074426f8","actionType":"REGISTRATION","status":"DRAFT",
                   "stateSyncStatus":"NOT_REQUIRED","externalRequestId":null,"registrationNumber":null,"reasonCode":null,
                   "reasonMessage":null,"sentAt":null,"completedAt":"2026-09-17T12:30:00Z"}]}"""
            ).applications.actions("07aeaff1").single()
        }
        val shift = runBlocking {
            cabinetReturning(
                """{"page":0,"size":50,"totalElements":1,"items":[{"shiftNumber":1,"status":"CLOSED",
                   "openedAt":"2026-09-07T10:05:06Z","closedAt":"2026-09-07T15:05:31Z","receiptsCount":1,
                   "saleTotal":690.00,"returnTotal":0,"buyTotal":400.00,"buyReturnTotal":100.00,
                   "cashTotal":690.00}]}"""
            ).documents.shifts("07aeaff1", 0).items.single()
        }
        assertEquals("074426f8", action.id)
        assertEquals("2026-09-17T12:30:00Z", action.processedAt)
        assertEquals("CLOSED", shift.state)
        assertEquals(1, shift.totals?.receiptsCount)
        // Покупка у населения доходит до карточки смены: без неё в кабинете
        // смена выглядела состоящей из одних продаж.
        assertEquals(Decimal.parse("400.00"), shift.totals?.purchasesSum)
        assertEquals(Decimal.parse("100.00"), shift.totals?.purchaseReturnsSum)
        assertEquals(Decimal.parse("690.00"), shift.totals?.revenue)
    }

    @Test
    fun `точка и адрес регистра несут казахскую форму`() {
        val place = runBlocking {
            cabinetReturning(
                """{"page":0,"size":50,"totalElements":1,"items":[{"id":"db5b84e9","name":"Магазин на Абая",
                   "addressRef":"0202247079279855","rka":"0202247079279855","cato":"751110000",
                   "address":"Алматы, Алмалинский, Абая, 1","addressKk":"Алматы, Алмалы, Абай, 1",
                   "latitude":43.238949,"longitude":76.889709,"cashRegisterCount":0}]}"""
            ).places.all().single()
        }
        val address = runBlocking {
            cabinetReturning(
                """{"rka":"0202247079279855","cato":"751110000","displayAddress":"Алматы, Алмалинский, Абая, 1",
                   "displayAddressKk":"Алматы, Алмалы, Абай, 1"}"""
            ).addresses.resolve("0202247079279855")
        }
        assertEquals("Алматы, Алмалы, Абай, 1", place.addressKz)
        assertEquals("0202247079279855", address.addressRef)
        assertEquals("Алматы, Алмалы, Абай, 1", address.addressKz)
    }

    @Test
    fun `токен со старшим битом читается беззнаковым и идёт с ключом идемпотентности`() {
        val issued = runBlocking {
            val cabinet = cabinetReturning("""{"kkmId":5000001,"token":-1234567890,"status":"ISSUED"}""")
            cabinet.registers.issueToken("07aeaff1")
        }
        assertEquals(3060399406L, issued.token)
        assertTrue(!seenHeaders["Idempotency-Key"].isNullOrBlank())
        assertEquals("Bearer rig-access", seenHeaders[HttpHeaders.Authorization], "ушло без доступа вошедшего")
    }

    /**
     * Отказ по полю: кабинет называет поле и говорит о нём по-русски,
     * а в `detail` ставит общее «Validation failure».
     *
     * Так он отвечает на неверный ввод — длину, диапазон, формат номера, —
     * и общее английское слово владельцу не говорит ничего: чинить нужно
     * то поле, о котором сказано в `errors`.
     */
    @Test
    fun `отказ по полю доходит словами о поле, а не общим Validation failure`() {
        val cabinet = cabinetReturning(
            """{"detail":"Validation failure","instance":"/api/cash-registers","status":400,
               "title":"Bad Request","errors":[{"field":"factoryNumber","message":"Заводской номер занят"}],
               "code":"VALIDATION_ERROR","timestamp":"2026-09-17T12:27:57Z","traceId":"05ba"}""",
            HttpStatusCode.BadRequest
        )
        val refusal = assertFailsWith<CabinetRefusal> { runBlocking { cabinet.registers.one("x") } }
        assertEquals("VALIDATION_ERROR", refusal.code)
        assertEquals("Заводской номер занят", refusal.text)
    }

    @Test
    fun `отказ кабинета читается из detail по RFC 9457`() {
        val cabinet = cabinetReturning(
            """{"detail":"Подпись не соответствует подписываемым данным","instance":"/api/cash-registers/x/registration/sign",
               "status":400,"title":"Bad Request","code":"SIGNATURE_INVALID","timestamp":"2026-09-17T12:27:57Z","traceId":"05ba"}""",
            HttpStatusCode.BadRequest
        )
        val refusal = assertFailsWith<CabinetRefusal> { runBlocking { cabinet.registers.one("x") } }
        assertEquals("SIGNATURE_INVALID", refusal.code)
        assertEquals("Подпись не соответствует подписываемым данным", refusal.text)
    }
}
