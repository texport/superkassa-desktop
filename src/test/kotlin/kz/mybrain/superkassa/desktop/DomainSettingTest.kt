package kz.mybrain.superkassa.desktop

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.content.TextContent
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.WorkplaceSettings
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.ui.sale.Basket
import kz.mybrain.superkassa.desktop.ui.sale.DomainInput
import kz.mybrain.superkassa.desktop.ui.sale.DomainKind
import kz.mybrain.superkassa.desktop.ui.sale.Position
import kz.mybrain.superkassa.desktop.ui.sale.SaleForm
import kz.mybrain.superkassa.desktop.ui.sale.issueReceipt
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.saleTextsRu
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Отрасль как настройка кассы.
 *
 * Прежде вид отрасли выбирался в каждом чеке, и кассир магазина
 * разбирался с полями такси. Теперь отрасль у кассы одна: она помнится
 * на рабочем месте и обязана доходить до запроса — протокол требует её
 * у каждого чека.
 */
class DomainSettingTest {

    @Test
    fun `по умолчанию касса работает в торговле`() {
        val settings = WorkplaceSettings(Preferences(freshFile()))
        assertEquals(DomainKind.Trading, settings.domain)
    }

    @Test
    fun `выбранная отрасль помнится на рабочем месте`() {
        val file = freshFile()

        WorkplaceSettings(Preferences(file)).chooseDomain(DomainKind.Taxi)

        assertEquals(DomainKind.Taxi, WorkplaceSettings(Preferences(file)).domain, "отрасль забылась")
        WorkplaceSettings(Preferences(file)).chooseDomain(DomainKind.Trading)
        assertEquals(DomainKind.Trading, WorkplaceSettings(Preferences(file)).domain)
    }

    /**
     * Настройка доходит до запроса чека — до того самого, который уходит
     * узлу, а не до промежуточного снимка экрана.
     */
    @Test
    fun `отрасль кассы уходит в запрос чека вместе с её реквизитами`() {
        val sent = mutableListOf<String>()
        val session = sessionThatTakesReceipts(sent)
        session.chooseDomain(DomainKind.Taxi)
        val basket = Basket().apply { add(POSITION) }
        val form = SaleForm().apply {
            domain = DomainInput(carNumber = "777ABC", isOrder = true, currentFee = "350")
        }

        runBlocking {
            issueReceipt(
                session = session,
                basket = basket,
                input = form.input(basket, session.domain),
                texts = stringsOf(Language.Ru),
                extra = saleTextsRu
            ) { "" }
        }

        val body = sent.single()
        assertTrue(body.contains("\"type\":\"DOMAIN_TAXI\""), "в запросе нет вида отрасли: $body")
        assertTrue(body.contains("\"carNumber\":\"777ABC\""), "в запросе нет номера машины: $body")
        assertTrue(!body.contains("\"parking\""), "в запросе оказался второй подблок: $body")
    }

    /** Чек, пробитый до смены настройки, не уносит её с собой в следующий. */
    @Test
    fun `набранные реквизиты забываются вместе с чеком`() {
        val form = SaleForm().apply { domain = DomainInput(carNumber = "777ABC", currentFee = "350") }

        form.startNextReceipt()

        assertEquals(DomainInput(), form.domain, "реквизиты покупателя ушли в следующий чек")
    }

    /**
     * Узел, принимающий чеки и запоминающий их тело.
     *
     * Вход кассира не нужен: пин и касса ставятся рабочему месту прямо,
     * а проверяется то, что уходит в запросе.
     */
    private fun sessionThatTakesReceipts(sent: MutableList<String>): Session {
        val engine = MockEngine { request ->
            if (request.method == HttpMethod.Post && request.url.encodedPath.contains(RECEIPT_PATH)) {
                sent.add((request.body as TextContent).text)
                respond(
                    """{"documentId":"d-1","deliveryStatus":"ONLINE_OK"}""",
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, JSON)
                )
            } else {
                respond(
                    """{"code":"NOT_FOUND","message":"RU: — | KK: — | EN: —"}""",
                    HttpStatusCode.NotFound,
                    headersOf(HttpHeaders.ContentType, JSON)
                )
            }
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(ServerClient.lenientJson) }
        }
        val session = Session(ServerClient(http = http), Preferences(freshFile()))
        session.adoptPin("1234")
        session.selected = KassaScene.kkm()
        return session
    }

    /** Свой каталог настроек у каждой проверки: общий затёр бы рабочую кассу. */
    private fun freshFile(): File =
        File(Files.createTempDirectory("superkassa-domain").toFile(), "kkm")

    private companion object {
        const val RECEIPT_PATH = "/receipt/"
        const val JSON = "application/json"

        val POSITION = Position(
            name = "Поездка",
            price = BigDecimal("1500.00"),
            quantity = BigDecimal("1"),
            vatGroup = "VAT_16",
            measureUnitCode = "796"
        )
    }
}
