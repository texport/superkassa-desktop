package kz.mybrain.superkassa.integrations.bfdcabinet

import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetFake.Reply
import kz.mybrain.superkassa.integrations.bfdcabinet.company.Oked
import kz.mybrain.superkassa.integrations.bfdcabinet.places.RetailPlaceAddress
import kz.mybrain.superkassa.integrations.bfdcabinet.places.RetailPlaceCreate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.ZERO

/**
 * Списки целиком, повтор выпуска токена и ответы, чья форма расходилась
 * с ожидаемой.
 */
class CabinetListsTest {

    /** Кабинет с [total] точками, отдающий их страницами по пятьдесят. */
    private fun places(total: Int, served: Int = total) = CabinetFake { request ->
        val page = request.url.parameters["page"]!!.toInt()
        val items = (page * CABINET_PAGE_SIZE until minOf(served, (page + 1) * CABINET_PAGE_SIZE))
            .joinToString(",") { """{"id":"p$it","name":"Точка $it"}""" }
        Reply("""{"page":$page,"size":50,"totalElements":$total,"items":[$items]}""")
    }

    /** У сети две тысячи точек, а до правки владелец видел пятьдесят первых. */
    @Test
    fun twoThousandPlacesAreReadAll() = runTest {
        val fake = places(total = 2000)
        val parts = mutableListOf<Int>()
        val all = fake.cabinet().places.all { part, total -> parts += part.size.also { assertEquals(2000L, total) } }

        assertEquals(2000, all.size)
        assertEquals(40, fake.asked.size)
        assertEquals(50, parts.first())
    }

    /** Точка заводится с местом на карте: без широты и долготы кабинет отвечает отказом без имён полей. */
    @Test
    fun placeIsCreatedWithItsPlaceOnTheMap() = runTest {
        val fake = CabinetFake.always("""{"id":"p-1","name":"Магазин на Абая"}""")
        fake.cabinet().places.add(RetailPlaceCreate("Магазин на Абая", "0202247079279855", LATITUDE, LONGITUDE))

        val body = fake.asked.single().text()
        assertTrue(""""latitude":43.238949""" in body && """"longitude":76.889709""" in body, body)
    }

    /**
     * Справочник моделей — как его отдаёт кабинет ECC: сто двадцать моделей,
     * и на всех страницах, кроме последней, итог `-1`. Чтение «пока прочитано
     * меньше итога» отдавало первые пятьдесят.
     */
    @Test
    fun modelsWithoutTotalAreReadToTheLastPage() = runTest {
        val fake = CabinetFake { request ->
            val page = request.url.parameters["page"]!!.toInt()
            val items = (page * CABINET_PAGE_SIZE until minOf(MODELS, (page + 1) * CABINET_PAGE_SIZE))
                .joinToString(",") { """{"modelCode":"0x0065$it","name":"«Модель $it»","active":true}""" }
            val total = if ((page + 1) * CABINET_PAGE_SIZE < MODELS) -1 else MODELS
            Reply("""{"page":$page,"size":50,"totalElements":$total,"items":[$items]}""")
        }

        assertEquals(MODELS, fake.cabinet().registers.models().size)
        assertEquals(3, fake.asked.size, "за последней страницей сходили ещё")
    }

    @Test
    fun promisedMoreThanServedDoesNotLoop() = runTest {
        val fake = places(total = 500, served = 120)

        assertEquals(120, fake.cabinet().places.all().size)
        assertEquals(4, fake.asked.size, "после пустой страницы чтение продолжилось")
    }

    /** Смена адреса отвечает HTTP 200 в обоих исходах; удавшаяся читалась отказом. */
    @Test
    fun addressChangeIsReadInBothOutcomes() = runTest {
        val direct = CabinetFake.always(
            """{"retailPlaceId":"b9da2db3","updated":true,"changeMode":"DIRECT",
               "affectedDraftCashRegisters":[],"blockingCashRegisters":[]}"""
        ).cabinet().places.move("place-1", RetailPlaceAddress("0201300118384402", LATITUDE, LONGITUDE))
        val blocked = CabinetFake.always(
            """{"retailPlaceId":"b9da2db3","updated":false,"changeMode":"REREGISTRATION_REQUIRED",
               "blockingCashRegisters":[{"id":"1","internalName":"Касса проверки","registrationNumber":"260940000031"},
               {"id":"2","registrationNumber":"260940000026"}]}"""
        ).cabinet().places.move("place-1", RetailPlaceAddress("0201300118384402", LATITUDE, LONGITUDE))

        assertTrue(direct.updated)
        assertFalse(direct.needsReregistration)
        assertTrue(blocked.needsReregistration)
        val blocking = blocked.blockingCashRegisters.map { it.registrationNumber }
        assertEquals(listOf("260940000031", "260940000026"), blocking)
    }

    @Test
    fun cardVersionsComeAsBareArray() = runTest {
        val versions = CabinetFake.always(
            """[{"versionNumber":2,"status":"REGISTERED","openedAt":"2026-09-18T09:12:00Z","closedAt":null,
                 "openedByActionType":"REREGISTRATION","changedFields":["retailPlace","address"],"active":true},
                {"versionNumber":1,"status":"REGISTERED","openedAt":"2026-05-02T11:00:00Z",
                 "closedAt":"2026-09-18T09:12:00Z","openedByActionType":"REGISTRATION",
                 "closedByActionType":"REREGISTRATION","changedFields":[],"active":false}]"""
        ).cabinet().cards.versions("r-1")

        assertEquals(listOf(2, 1), versions.map { it.version })
        assertEquals(listOf(true, false), versions.map { it.open })
        assertEquals("REREGISTRATION", versions[1].closedBy)
    }

    /**
     * Токен, не подтверждённый сервисом приёма, спрашивается снова тем же ключом.
     *
     * Неподтверждённый ответ — как у кабинета: `"token": null`, а не ноль.
     */
    @Test
    fun pendingTokenIsAskedAgainWithSameKey() = runTest {
        var calls = 0
        val fake = CabinetFake {
            calls++
            Reply(if (calls < 3) """{"kkmId":1,"token":null,"status":"PENDING"}""" else ISSUED)
        }
        val issued = fake.cabinet().registers.issueToken("r-1")

        assertEquals(77L, issued.token)
        assertEquals(3, fake.asked.size)
        assertEquals(1, fake.asked.map { it.headers["Idempotency-Key"] }.distinct().size)
        assertEquals(HttpMethod.Post, fake.asked.first().method)
    }

    /** Не дождались подтверждения — токена нет, а не ноль, который касса приняла бы за токен. */
    @Test
    fun unconfirmedTokenHasNoValue() = runTest {
        val fake = CabinetFake.always("""{"kkmId":1,"token":null,"status":"PENDING"}""")
        val issued = fake.cabinet(CabinetSettings(tokenAttempts = 2, tokenPause = ZERO)).registers.issueToken("r-1")

        assertTrue(issued.pending)
        assertNull(issued.token)
        assertEquals(2, fake.asked.size)
    }

    /** Кабинет ждёт `primary` всегда: без него второй вид деятельности отвергался. */
    @Test
    fun secondaryOkedIsSentWithExplicitFlag() = runTest {
        val fake = CabinetFake.always(
            """{"id":"c","okeds":[{"code":"47111","primary":true},{"code":"56101","primary":false}]}"""
        )
        val saved = fake.cabinet().company.saveOkeds(listOf(Oked("47111", primary = true), Oked("56101")))

        assertTrue(fake.asked.single().text().contains(""""code":"56101","primary":false"""))
        assertEquals(2, saved.size)
    }

    @Test
    fun okedsAreAskedWithOffsetAndTotal() = runTest {
        val fake = CabinetFake.always("""{"items":[{"code":"47111","name":"Розничная торговля"}],"total":2107}""")
        val found = fake.cabinet().company.okeds("торговля", from = 50)

        assertEquals(2107L, found.total)
        assertEquals("торговля", fake.asked.single().url.parameters["query"])
        assertEquals("limit=50&offset=50", fake.asked.single().url.encodedQuery.substringAfter('&'))
        fake.cabinet().company.okeds("")
        assertEquals("/api/reference/okeds?limit=50&offset=0", fake.asked.last().target())
    }

    /**
     * Название кассы уходит полем всегда: кабинет требует его (`@NotBlank`),
     * и тело без поля — `{}` при пропуске `null` — он отвергал проверкой полей.
     */
    @Test
    fun renameSendsTheNameField() = runTest {
        val fake = CabinetFake.always("""{"id":"r-1","kkmId":5000001,"internalName":"У входа","status":"DRAFT"}""")
        val renamed = fake.cabinet().registers.rename("r-1", "У входа")

        assertEquals(HttpMethod.Patch, fake.asked.single().method)
        assertEquals("/api/cash-registers/r-1/internal-name", fake.asked.single().target())
        assertEquals("""{"internalName":"У входа"}""", fake.asked.single().text())
        assertEquals("У входа", renamed.internalName)
    }

    private companion object {
        const val ISSUED = """{"kkmId":1,"token":77,"status":"ISSUED"}"""

        val LATITUDE = CabinetDecimal.of("43.238949")
        val LONGITUDE = CabinetDecimal.of("76.889709")

        /** Сколько моделей в справочнике кабинета ECC. */
        const val MODELS = 120
    }
}
