package kz.mybrain.superkassa.integrations.bfdcabinet

import io.ktor.http.HttpHeaders
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Живые ответы кабинета `bfd-cabinet.ecc.kz`, снятые 2026-09-17, разбираются
 * в DTO модуля. Тела взяты без правок: поменяет кабинет схему — проверка
 * покажет, что именно перестало читаться.
 */
class CabinetContractTest {

    private val development = CabinetSettings(development = DevelopmentIdentity("900101300000", "230140000000"))

    @Test
    fun developmentIdentityGoesInHeadersInsteadOfAccess() = runTest {
        val fake = CabinetFake.always(
            """{"user":{"id":"987afff6","iin":"900101300000","fullName":"Курманов Азамат Бахытжанович"},
               "company":{"id":"939158cd","bin":"230140000000","name":"ТОО Азик и Ко"},
               "expiresAt":"2026-09-17T20:24:20.751175Z"}"""
        )
        val owner = fake.unsigned(development).account.signIn()

        assertEquals("230140000000", owner.company.bin)
        val request = fake.asked.single()
        assertEquals("/api/auth/me", request.url.encodedPath)
        assertEquals("900101300000", request.headers[DevelopmentIdentity.IIN_HEADER])
        assertEquals("230140000000", request.headers[DevelopmentIdentity.BIN_HEADER])
        assertNull(request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun cardAndListRowGiveModelAndPlaceAlike() = runTest {
        val card = CabinetFake.always(
            """{"id":"07aeaff1","kkmId":5000001,"internalName":"Касса демо","status":"DRAFT","registrationNumber":null,
               "factoryNumber":"SN-ECC-172758","manufactureYear":2026,
               "model":{"modelCode":"0x0065000086cb","name":"«ПОРТ FPG-350 ФKZ»"},
               "retailPlace":{"id":"db5b84e9","name":"Магазин на Абая"},
               "lastRegistrationAction":{"actionId":"074426f8","actionType":"REGISTRATION","status":"DRAFT",
               "stateSyncStatus":"NOT_REQUIRED","sentAt":null,"completedAt":null},
               "registrationCardAvailable":false}"""
        ).cabinet().registers.one("07aeaff1")
        val listed = CabinetFake.always(
            """{"page":0,"size":5,"totalElements":1,"items":[{"id":"07aeaff1","kkmId":5000001,
            "internalName":"Касса демо",
               "status":"DRAFT","registrationNumber":null,"factoryNumber":"SN-ECC-172758",
               "modelName":"«ПОРТ FPG-350 ФKZ»",
               "retailPlaceId":"db5b84e9","retailPlaceName":"Магазин на Абая"}]}"""
        ).cabinet().registers.page().items.single()

        assertEquals("«ПОРТ FPG-350 ФKZ»", card.model?.name)
        assertEquals("«ПОРТ FPG-350 ФKZ»", listed.model?.name)
        assertEquals("db5b84e9", card.retailPlace?.id)
        assertEquals("db5b84e9", listed.retailPlace?.id)
        assertEquals("REGISTRATION", card.lastRegistrationAction?.type)
    }

    @Test
    fun stateOfRegisterUnknownToServerIsNotFound() = runTest {
        val state = CabinetFake.always(
            """{"cashRegisterId":"07aeaff1","businessStatus":"DRAFT","stateSyncStatus":"NOT_REQUIRED",
               "technicalState":{"found":false,"active":false,"inactiveReason":"NOT_REGISTERED","shiftStatus":"UNKNOWN",
               "shiftNumber":null,"validationMask":null,"lastContactAt":null}}"""
        ).cabinet().registers.state("07aeaff1")

        assertEquals(false, state.technicalState?.found)
        assertEquals("NOT_REGISTERED", state.technicalState?.inactiveReason)
    }

    @Test
    fun actionsAndShiftsAreReadByCabinetNames() = runTest {
        val action = CabinetFake.always(
            """{"page":0,"size":5,"totalElements":1,"items":[{"actionId":"074426f8",
            "actionType":"REGISTRATION","status":"DRAFT",
               "stateSyncStatus":"NOT_REQUIRED","externalRequestId":null,"registrationNumber":null,"reasonCode":null,
               "reasonMessage":null,"sentAt":null,"completedAt":"2026-09-17T12:30:00Z"}]}"""
        ).cabinet().applications.actions("07aeaff1").single()
        val shift = CabinetFake.always(
            """{"page":0,"size":50,"totalElements":1,"items":[{"shiftNumber":1,"status":"CLOSED",
               "openedAt":"2026-09-07T10:05:06Z","closedAt":"2026-09-07T15:05:31Z","receiptsCount":1,
               "saleTotal":690.00,"returnTotal":0,"buyTotal":400.00,"buyReturnTotal":100.00,
               "cashTotal":690.00}]}"""
        ).cabinet().documents.shifts("07aeaff1").items.single()

        assertEquals("074426f8", action.id)
        assertEquals("2026-09-17T12:30:00Z", action.processedAt)
        assertEquals("CLOSED", shift.state)
        assertEquals(1, shift.receiptsCount)
        assertEquals(CabinetDecimal.of("400"), shift.buyTotal)
        assertEquals(10_000L, shift.buyReturnTotal?.tiyn())
        assertEquals("690.00", shift.saleTotal?.plain)
    }

    @Test
    fun placeAndRegistryAddressCarryKazakhForm() = runTest {
        val place = CabinetFake.always(
            """{"page":0,"size":50,"totalElements":1,"items":[{"id":"db5b84e9","name":"Магазин на Абая",
               "addressRef":"0202247079279855","rka":"0202247079279855","cato":"751110000",
               "address":"Алматы, Алмалинский, Абая, 1","addressKk":"Алматы, Алмалы, Абай, 1",
               "latitude":43.238949,"longitude":76.889709,"cashRegisterCount":0}]}"""
        ).cabinet().places.page().items.single()
        val address = CabinetFake.always(
            """{"rka":"0202247079279855","cato":"751110000","displayAddress":"Алматы, Алмалинский, Абая, 1",
               "displayAddressKk":"Алматы, Алмалы, Абай, 1"}"""
        ).cabinet().addresses.resolve("0202247079279855")

        assertEquals("Алматы, Алмалы, Абай, 1", place.addressKz)
        assertEquals("43.238949", place.latitude?.plain)
        assertEquals("0202247079279855", address.addressRef)
        assertEquals("Алматы, Алмалы, Абай, 1", address.addressKz)
    }

    @Test
    fun tokenWithHighBitIsUnsignedAndCarriesIdempotencyKey() = runTest {
        val fake = CabinetFake.always("""{"kkmId":5000001,"token":-1234567890,"status":"ISSUED"}""")
        val issued = fake.cabinet().registers.issueToken("07aeaff1")

        assertEquals(3_060_399_406L, issued.token)
        assertTrue(!fake.asked.single().headers["Idempotency-Key"].isNullOrBlank())
        assertEquals("Bearer access-1", fake.asked.single().headers[HttpHeaders.Authorization])
    }

    /** На неверный ввод кабинет говорит о поле в `errors`, а в `detail` — общее «Validation failure». */
    @Test
    fun fieldRefusalComesInWordsAboutTheField() = runTest {
        val fake = CabinetFake.always(
            """{"detail":"Validation failure","instance":"/api/cash-registers","status":400,
               "title":"Bad Request","errors":[{"field":"factoryNumber","message":"Заводской номер занят"}],
               "code":"VALIDATION_ERROR","timestamp":"2026-09-17T12:27:57Z","traceId":"05ba"}""",
            status = 400
        )
        val refusal = assertFailsWith<CabinetRefusal> { fake.cabinet().registers.one("x") }

        assertEquals("VALIDATION_ERROR", refusal.code)
        assertEquals("Заводской номер занят", refusal.text)
    }

    @Test
    fun refusalIsReadFromDetailByRfc9457() = runTest {
        val fake = CabinetFake.always(
            """{"detail":"Подпись не соответствует подписываемым данным",
            "instance":"/api/cash-registers/x/registration/sign",
               "status":400,"title":"Bad Request","code":"SIGNATURE_INVALID",
               "timestamp":"2026-09-17T12:27:57Z","traceId":"05ba"}""",
            status = 400
        )
        val refusal = assertFailsWith<CabinetRefusal> { fake.cabinet().registers.one("x") }

        assertEquals("SIGNATURE_INVALID", refusal.code)
        assertEquals("Подпись не соответствует подписываемым данным", refusal.text)
    }
}
