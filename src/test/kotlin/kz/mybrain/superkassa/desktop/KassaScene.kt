package kz.mybrain.superkassa.desktop

import androidx.compose.runtime.Composable
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.CounterRecord
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.DictionaryEntry
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.DocumentDetails
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.KkmUser
import kz.mybrain.superkassa.desktop.server.NodeVatRate
import kz.mybrain.superkassa.desktop.server.OrgInfo
import kz.mybrain.superkassa.desktop.server.ServerClient
import kz.mybrain.superkassa.desktop.server.SoldItem
import kz.mybrain.superkassa.desktop.server.Trilingual
import kz.mybrain.superkassa.desktop.server.UnitOfMeasurement
import kz.mybrain.superkassa.desktop.ui.history.Shift
import kz.mybrain.superkassa.desktop.ui.strings.Language
import java.io.File
import java.nio.file.Files

/** Отказ узла для снимка: код и слова на трёх языках, как их отдаёт узел. */
internal data class NodeRefusal(val code: String, val ru: String, val kk: String, val en: String)

/**
 * Оснастка кассовых снимков.
 *
 * Экраны кассы спрашивают рабочее место, а не узел напрямую, поэтому сцене
 * нужен настоящий [Session] — только с узлом, которого нет. Каталог настроек
 * у каждого снимка свой: прогон проверок однажды затёр настройки рабочей
 * кассы, и в общем каталоге это повторилось бы.
 */
internal object KassaScene {

    /** Касса, какой её видит кассир на рабочем месте. */
    fun kkm(
        state: String? = "ACTIVE",
        kgd: String? = "000000200042",
        name: String? = "Касса у входа",
        autonomousSince: Long? = null,
        taxRegime: String? = "GENERAL"
    ) = Kkm(
        kkmId = "kkm-1",
        name = name,
        kkmKgdId = kgd,
        factoryNumber = "SK-000042",
        state = state,
        autonomousSince = autonomousSince,
        taxRegime = taxRegime,
        ofdServiceInfo = OrgInfo(orgTitle = "ТОО «Пример»", orgAddress = "Алматы, Абая 150")
    )

    /**
     * Рабочее место с выбранной кассой и принятым пином.
     *
     * Пин принимается через вход: узел отвечает кассиром только на запрос
     * «кто я», и другого способа получить роль у сеанса нет.
     */
    fun session(
        folder: String,
        kkm: Kkm? = kkm(),
        admin: Boolean = true,
        available: Boolean = true,
        shift: Shift? = null,
        shiftAnswered: Boolean = true,
        documents: List<Document> = emptyList(),
        cashInDrawerTiyn: Long? = 125_000,
        payments: List<DictionaryEntry>? = null,
        refusal: NodeRefusal? = null,
        journal: List<Document> = emptyList(),
        sold: List<SoldItem> = emptyList(),
        cashiers: List<KkmUser>? = null
    ): Session {
        val session = Session(client(admin, refusal, journal, sold, cashiers), preferences(folder))
        // Язык сеанса тот же, каким сцена рисует надписи: иначе отказ узла
        // приходил по-казахски на русский экран — не дефект приложения,
        // а расхождение оснастки с ним.
        session.switchLanguage(Language.Ru)
        if (available) session.calls.answered()
        kkm?.let { runBlocking { session.signIn(it, PIN) } }
        if (shiftAnswered) session.board.adoptShift(shift)
        session.board.adoptDocuments(documents)
        cashInDrawerTiyn?.let {
            session.board.adoptCounters(listOf(CounterRecord(scope = "GLOBAL", key = "cash.sum", value = it)))
        }
        session.reference.adoptUnits(UNITS)
        session.reference.adoptVatRates(VAT_RATES)
        payments?.let { session.dictionaries[Dictionary.PaymentTypes] = it }
        session.dictionaries[Dictionary.DocumentTypes] = DOCUMENT_TYPES
        session.dictionaries[Dictionary.DeliveryStatuses] = DELIVERY_STATUSES
        session.dictionaries[Dictionary.KkmStates] = KKM_STATES
        return session
    }

    /** Открытая смена с этим номером. */
    fun openShift(number: Long = 7, openedAt: Long = System.currentTimeMillis()) =
        Shift(id = "shift-$number", shiftNo = number, status = "OPEN", openedAt = openedAt)

    /**
     * Кадр экрана в файл, чтобы смотреть глазами.
     *
     * Кадров несколько, а сохраняется последний: снекбар отказа, ожидание
     * и появление списка живут в отложенных действиях, и на первом кадре
     * их на экране ещё нет — отказной снимок выходил неотличимым
     * от обычного.
     */
    fun shot(
        name: String,
        width: Int = WIDE,
        height: Int = TALL,
        settle: Int = SETTLE,
        content: @Composable () -> Unit
    ): ByteArray {
        val frame = RenderProbe(width = width, height = height, content = content).use { probe ->
            repeat(settle) { probe.frame() }
            probe.frame()
        }
        File("/tmp/kassa-$name.png").writeBytes(frame)
        return frame
    }

    private fun preferences(folder: String): Preferences {
        val directory = Files.createTempDirectory(folder).toFile()
        return Preferences(File(directory, "kkm"))
    }

    /**
     * Узел, который отвечает только на «кто я».
     *
     * Всё прочее — отказ: отказные снимки нужны не меньше обычных, а
     * поднимать узел ради картинки нельзя. Отказ отдаётся в том же виде,
     * в каком его отдаёт узел, — кодом и трёхъязычной строкой: с голым
     * `respondError` на экране оказывалось английское «Not Found»,
     * то есть не то, что кассир увидит на самом деле.
     */
    private fun client(
        admin: Boolean,
        refusal: NodeRefusal?,
        journal: List<Document>,
        sold: List<SoldItem>,
        cashiers: List<KkmUser>?
    ): ServerClient {
        val body = """{"userId":"u-1","name":"Айгүл Сәрсенова","role":"${if (admin) "ADMIN" else "CASHIER"}"}"""
        val engine = MockEngine { request ->
            val path = request.url.encodedPath
            when {
                path.endsWith("/users/me") -> answer(body)
                path.endsWith("/users") && cashiers != null ->
                    answer(encoded(ListSerializer(KkmUser.serializer()), cashiers))

                path.endsWith("/documents") -> answer(encoded(ListSerializer(Document.serializer()), journal))
                path.contains("/documents/") -> answer(details(journal, sold, path.substringAfterLast('/')))
                refusal != null -> respond(
                    """{"code":"${refusal.code}","message":"RU: ${refusal.ru} | KK: ${refusal.kk} | EN: ${refusal.en}"}""",
                    HttpStatusCode.BadRequest,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )

                else -> respondError(HttpStatusCode.NotFound)
            }
        }
        val http = HttpClient(engine) {
            expectSuccess = false
            install(ContentNegotiation) { json(ServerClient.lenientJson) }
        }
        return ServerClient(http = http)
    }

    /** Ответ узла по существу: телом идёт готовый JSON. */
    private fun MockRequestHandleScope.answer(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))

    private fun <T> encoded(serializer: KSerializer<T>, value: T): String =
        ServerClient.lenientJson.encodeToString(serializer, value)

    /** Состав документа: позиции те же, что продавались, и кассир при них. */
    private fun details(journal: List<Document>, sold: List<SoldItem>, id: String): String {
        val document = journal.firstOrNull { it.id == id } ?: Document(id = id)
        return encoded(
            DocumentDetails.serializer(),
            DocumentDetails(document = document, items = sold, operatorName = "Айгүл Сәрсенова")
        )
    }

    const val PIN = "1234"

    /** Окно кассира на рабочем месте: столько точек даёт каркас разделу. */
    const val WIDE = 1180
    const val TALL = 820

    /** Сколько кадров даётся отложенным действиям, чтобы доехать до экрана. */
    private const val SETTLE = 40

    private val UNITS = listOf(
        UnitOfMeasurement(code = "796", nameShort = "шт", nameFull = "Штука"),
        UnitOfMeasurement(code = "166", nameShort = "кг", nameFull = "Килограмм")
    )

    private val VAT_RATES = listOf(
        NodeVatRate(code = "VAT_NO", percent = 0, name = Trilingual(ru = "Без НДС", kk = "ҚҚС-сыз", en = "No VAT")),
        NodeVatRate(code = "VAT_16", percent = 16, name = Trilingual(ru = "НДС 16%", kk = "ҚҚС 16%", en = "VAT 16%"))
    )

    private val DOCUMENT_TYPES = listOf(
        entry("SALE", "Продажа"),
        entry("BUY", "Покупка"),
        entry("RETURN", "Возврат продажи"),
        entry("CASH_IN", "Внесение"),
        entry("CASH_OUT", "Изъятие"),
        entry("X_REPORT", "X-отчёт"),
        entry("Z_REPORT", "Z-отчёт"),
        entry("SHIFT_OPEN", "Открытие смены")
    )

    private val DELIVERY_STATUSES = listOf(
        entry("ONLINE_OK", "Доставлен"),
        entry("OFFLINE_QUEUED", "Ждёт отправки")
    )

    private val KKM_STATES = listOf(
        entry("ACTIVE", "Работает"),
        entry("BLOCKED", "Заблокирована"),
        entry("PROGRAMMING", "Программирование")
    )

    private fun entry(code: String, ru: String) = DictionaryEntry(code = code, name = mapOf("ru" to ru))
}
