package kz.mybrain.superkassa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.ui.sale.NO_VAT
import kz.mybrain.superkassa.desktop.ui.sale.defaultVatOf
import kz.mybrain.superkassa.desktop.ui.sale.vatAllowed
import kz.mybrain.superkassa.desktop.ui.sale.vatRatesOf
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Ставки НДС, показанные кассиру, обязаны дойти до ОФД.
 *
 * Касса в режиме «Без НДС» давала выбрать ставку в позиции чека, писала
 * «НДС 12 %» в строке корзины и печатала её, а в ОФД уходила позиция без
 * налога: в пакете узла рядом лежали "vatGroup":"VAT_12",
 * "taxRegime":"NO_VAT" и "ticketTaxes":[]. Выбора, который никуда
 * не уходит, быть не должно.
 */
class SaleVatRegimeTest {

    private val enums = stringsOf(Language.Ru).enums

    private fun sessionOn(kkm: Kkm): Session {
        val http = HttpClient(MockEngine { error("узел в этом тесте не нужен") }) {
            expectSuccess = false
            install(ContentNegotiation) { json(ServerClient.lenientJson) }
        }
        val directory = Files.createTempDirectory("superkassa-vat").toFile()
        directory.deleteOnExit()
        val session = Session(ServerClient(http = http), Preferences(File(directory, "kkm")))
        session.select(kkm, remember = false)
        return session
    }

    private fun kkmWith(regime: String, group: String) = Kkm(
        kkmId = "4166498c-d0c1-406e-863d-20458dfd3040",
        kkmKgdId = "260940000021",
        state = "ACTIVE",
        taxRegime = regime,
        defaultVatGroup = group
    )

    @Test
    fun `неплательщику НДС ставок не предлагают`() {
        val session = sessionOn(kkmWith("NO_VAT", NO_VAT))

        val rates = vatRatesOf(session, enums)

        assertFalse(vatAllowed(session))
        assertEquals(listOf(NO_VAT), rates.map { it.code })
    }

    @Test
    fun `плательщику НДС перечень ставок остаётся`() {
        val session = sessionOn(kkmWith("VAT_PAYER", "VAT_16"))

        val rates = vatRatesOf(session, enums)

        assertTrue(vatAllowed(session))
        assertTrue(rates.size > 1, rates.map { it.code }.toString())
        assertTrue(rates.any { it.code == "VAT_16" })
    }

    /** Ставка новой позиции берётся из перечня, а он уже сужен режимом. */
    @Test
    fun `новая позиция неплательщика начинается без НДС`() {
        val session = sessionOn(kkmWith("NO_VAT", "VAT_12"))

        val rates = vatRatesOf(session, enums)

        assertEquals(NO_VAT, defaultVatOf(session, rates))
    }

    /** Режим не назван — ставки остаются: прятать их у плательщика нельзя. */
    @Test
    fun `касса без названного режима ставок не теряет`() {
        val session = sessionOn(Kkm(kkmId = "a1", state = "ACTIVE"))

        assertTrue(vatAllowed(session))
        assertTrue(vatRatesOf(session, enums).size > 1)
    }
}
