package kz.mybrain.superkassa.presentation.settings.ofd

import io.github.texport.superkassa.core.domain.api.model.settings.CoreMode
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.api.model.settings.StorageSettings
import io.github.texport.superkassa.core.presentation.api.model.ofd.OfdCommandResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kz.mybrain.superkassa.domain.settings.model.KassaFacts
import kz.mybrain.superkassa.domain.settings.model.OfdSummary
import kz.mybrain.superkassa.presentation.settings.core.CoreSettingsUiState
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Сведения от ОФД и о самой кассе, сведённые к тому, что читает кассир.
 *
 * Касса отвечает на эти запросы целиком: двоичный пакет, заголовок с токеном,
 * Z-отчёт, а в настройках — ещё путь к базе и учётные данные хранилища.
 * Ни одно из этого на экран попасть не должно, и проверяется это здесь.
 */
class MoneyOfdSummaryTest {

    private val texts = textsOf(Language.Ru).kassa.money.kkm

    private val answer = """
        {
          "status": "OK",
          "responseBin": [-94, -127, -52, 0, 32, 3, 0, 0],
          "responseJson": {
            "ofdId": "bfd",
            "protocolVersion": "204",
            "header": {"size": 800, "deviceId": 2000043, "token": 3295187247, "reqNum": 21},
            "payload": {
              "result": {
                "resultCode": 0,
                "resultType": {
                  "code": 0,
                  "descriptionRu": "Команда выполнена успешно.",
                  "descriptionKz": "Пәрмен сәтті орындалды.",
                  "descriptionEn": "Command executed successfully."
                }
              },
              "service": {
                "regInfo": {
                  "kkm": {"fnsKkmId": "KGD-2000043", "serialNumber": "SN-2000043", "kkmId": "2000043"},
                  "org": {
                    "title": "Организация",
                    "address": "Алматы",
                    "addressKz": "Алматы қаласы",
                    "inn": "123456789012"
                  }
                }
              }
            }
          },
          "responseToken": 3295187247
        }
    """.trimIndent()

    @Test
    fun `из ответа берутся регистрационные сведения кассы`() {
        val summary = OfdSummary.of(ofd(answer), "ru")

        assertEquals("KGD-2000043", summary.kgdNumber)
        assertEquals("SN-2000043", summary.factoryNumber)
        assertEquals("2000043", summary.systemId)
        assertEquals("204", summary.protocol)
        assertEquals("Организация", summary.organization)
        assertEquals("Алматы", summary.address)
        assertEquals("123456789012", summary.bin)
        assertEquals("Команда выполнена успешно.", summary.answer)
    }

    @Test
    fun `ответ и адрес показываются на языке кассира`() {
        assertEquals("Пәрмен сәтті орындалды.", OfdSummary.of(ofd(answer), "kk").answer)
        assertEquals("Алматы қаласы", OfdSummary.of(ofd(answer), "kk").address)
        assertEquals("Command executed successfully.", OfdSummary.of(ofd(answer), "en").answer)
    }

    @Test
    fun `протокольный пакет и токен на экран не попадают`() {
        val shown = OfdSummary.of(ofd(answer), "ru").rows(texts).joinToString(" ") { "${it.first} ${it.second}" }

        assertFalse(shown.contains("3295187247"), "на экране оказался токен: $shown")
        assertFalse(shown.contains("-94"), "на экране оказался двоичный пакет: $shown")
        assertFalse(shown.contains("reqNum"), "на экране оказался заголовок пакета: $shown")
    }

    @Test
    fun `неразобранный ответ — пустая сводка, а не падение`() {
        assertTrue(OfdSummary.of(null, "ru").rows(texts).isEmpty())
        assertTrue(OfdSummary.of(JsonObject(emptyMap()), "ru").rows(texts).isEmpty())
        assertNull(OfdSummary.of(ofd("""{"status":"FAILED"}"""), "ru").organization)
    }

    @Test
    fun `о кассе показывается режим, протокол и хранилище`() {
        val facts = CoreSettingsUiState(settings).facts(about, core, texts).toMap()

        assertEquals(core.modeDesktop, facts[core.mode])
        assertEquals("2.0.4", facts[texts.bfdProtocol], "версия протокола — как в документах")
        assertEquals("${texts.storageLocal} (SQLite)", facts[texts.kassaStorage], "хранилище — именем перечисления")
    }

    @Test
    fun `путь к базе и учётные данные хранилища на экран не попадают`() {
        val state = CoreSettingsUiState(settings, KassaFacts("1.5.0", "1.5.0", "/home/kassa/.superkassa/kassa", 2))
        val shown = state.facts(about, core, texts).joinToString(" ") { "${it.first} ${it.second}" }

        assertFalse(shown.contains("jdbc"), "на экране оказался путь к базе: $shown")
        assertFalse(shown.contains("4821"), "на экране оказался пароль хранилища: $shown")
    }

    @Test
    fun `о кассе показываются версии, каталог данных и число касс, как прежде у узла`() {
        val state = CoreSettingsUiState(settings, KassaFacts("1.4.2", "1.5.0-SNAPSHOT", "/data/kassa", 3))
        val facts = state.facts(about, core, texts).toMap()

        assertEquals("1.4.2", facts[about.appVersion])
        assertEquals("1.5.0-SNAPSHOT", facts[about.coreVersion])
        assertEquals("/data/kassa", facts[about.dataDirectory])
        assertEquals("3", facts[about.kkmCount])
    }

    @Test
    fun `версии видны, даже когда касса не отдала настройки и число касс`() {
        val facts = CoreSettingsUiState(about = KassaFacts("1.4.2", "1.5.0", null, null)).facts(about, core, texts)

        assertEquals(listOf(about.appVersion, about.coreVersion), facts.map { it.first })
    }

    /** Ответ ОФД так, как его отдаёт касса: разобранный JSON внутри результата команды. */
    private fun ofd(body: String): JsonObject? =
        lenient.decodeFromString(OfdCommandResponse.serializer(), body).responseJson

    private val lenient = Json { ignoreUnknownKeys = true }

    private val core = textsOf(Language.Ru).settings.core

    private val about = textsOf(Language.Ru).settings.facts

    private val settings = CoreSettings(
        mode = CoreMode.DESKTOP,
        storage = StorageSettings("SQLITE", "jdbc:sqlite:data/core.db", user = "kassa", password = "4821"),
        ofdProtocolVersion = "204",
        ofdTimeoutSeconds = 15
    )
}
