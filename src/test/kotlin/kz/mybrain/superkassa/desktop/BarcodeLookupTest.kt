package kz.mybrain.superkassa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.server.lookupBarcode
import kz.mybrain.superkassa.desktop.ui.sale.LookupProblem
import kz.mybrain.superkassa.desktop.ui.sale.lookupProblemOf
import kz.mybrain.superkassa.desktop.ui.sale.lookupProblemWords
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.blockReasonTexts
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * Поиск по штрихкоду называет кассиру то, что случилось.
 *
 * Товара нет в справочнике, справочник не спросить, касса заблокирована —
 * три разные беды. Прежде все три кончались одной фразой «в справочнике
 * нет такого штрихкода», и кассир искал несуществующую беду с товаром
 * вместо потерянной связи.
 */
class BarcodeLookupTest {

    @Test
    fun `каталог ответил и товара в нём нет — это отсутствие`() = runBlocking {
        val session = sessionAnswering(HttpStatusCode.NotFound, NOMENCLATURE_NOT_FOUND, "Товар не найден")

        val found = session.guard(SEARCH) { session.client.lookupBarcode(KKM, BARCODE, PIN) }

        assertNull(found)
        assertNull(session.lastMessage)
        assertEquals(LookupProblem.Missing, lookupProblemOf(session))
        assertEquals(sale(session).barcodeMissing, words(session))
    }

    @Test
    fun `молчащий справочник не выдаётся за отсутствие товара`() = runBlocking {
        val session = sessionAnswering(
            HttpStatusCode.ServiceUnavailable,
            "NOMENCLATURE_UNAVAILABLE",
            "Справочник сейчас недоступен"
        )

        val found = session.guard(SEARCH) { session.client.lookupBarcode(KKM, BARCODE, PIN) }

        assertNull(found)
        assertEquals(LookupProblem.Unavailable, lookupProblemOf(session))
        assertEquals(sale(session).barcodeUnavailable, words(session))
        assertNotEquals(sale(session).barcodeMissing, words(session))
    }

    @Test
    fun `узел не ответил вовсе — тоже недоступный справочник`() = runBlocking {
        val session = sessionWith(MockEngine { throw java.io.IOException("node is silent") })

        val found = session.guard(SEARCH) { session.client.lookupBarcode(KKM, BARCODE, PIN) }

        assertNull(found)
        assertEquals(LookupProblem.Unavailable, lookupProblemOf(session))
        assertEquals(sale(session).barcodeUnavailable, words(session))
    }

    @Test
    fun `заблокированная касса названа блокировкой и её причиной`() = runBlocking {
        val session = sessionAnswering(HttpStatusCode.BadRequest, "KKM_BLOCKED", "Касса заблокирована")
        session.selected = KkmScene.blocked(INVALID_TOKEN)

        val found = session.guard(SEARCH) { session.client.lookupBarcode(KKM, BARCODE, PIN) }

        assertNull(found)
        assertEquals(LookupProblem.Blocked, lookupProblemOf(session))
        assertEquals(blockReasonTexts(Language.Ru).invalidToken, words(session))
        assertNotEquals(sale(session).barcodeMissing, words(session))
    }

    /** Ненайденная касса — не ненайденный товар: её отсутствие остаётся отказом. */
    @Test
    fun `отсутствие самой кассы не выдаётся за отсутствие товара`() = runBlocking {
        val session = sessionAnswering(HttpStatusCode.NotFound, "KKM_NOT_FOUND", "Касса не найдена")

        val found = session.guard(SEARCH) { session.client.lookupBarcode(KKM, BARCODE, PIN) }

        assertNull(found)
        assertEquals(LookupProblem.Unavailable, lookupProblemOf(session))
        assertNotEquals(sale(session).barcodeMissing, words(session))
    }

    private fun words(session: Session): String =
        lookupProblemWords(lookupProblemOf(session), session, sale(session))

    private fun sale(session: Session) = stringsOf(session.language).sale

    private fun sessionAnswering(status: HttpStatusCode, code: String, ru: String): Session = sessionWith(
        MockEngine {
            respond(
                """{"code":"$code","message":"RU: $ru | KK: $ru | EN: $ru"}""",
                status,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
    )

    private fun sessionWith(engine: MockEngine): Session {
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(ServerClient.lenientJson) }
        }
        val directory = Files.createTempDirectory("superkassa-barcode").toFile()
        directory.deleteOnExit()
        val session = Session(ServerClient(http = http), Preferences(File(directory, "kkm")))
        session.switchLanguage(Language.Ru)
        return session
    }
}

/** Касса, какой её видит поиск: заблокированная, с названной причиной. */
private object KkmScene {
    fun blocked(reason: Int) = Kkm(kkmId = KKM, state = "BLOCKED", blockReasonCode = reason)
}

private const val KKM = "kkm-1"
private const val PIN = "1234"
private const val BARCODE = "5449000176431"
private const val SEARCH = "Поиск по штрихкоду"
private const val NOMENCLATURE_NOT_FOUND = "NOMENCLATURE_NOT_FOUND"

/** Отказ БФД «неверный токен», перенесённый узлом в свой диапазон. */
private const val INVALID_TOKEN = 1002
