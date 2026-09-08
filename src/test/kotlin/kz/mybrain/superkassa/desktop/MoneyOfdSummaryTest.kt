package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.settings.parseNodeSettings
import kz.mybrain.superkassa.desktop.ui.settings.parseOfdInfo
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.moneyTexts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Сведения от ОФД и об узле, сведённые к тому, что читает кассир.
 *
 * Узел отвечает на эти запросы целиком: двоичный пакет, заголовок с токеном,
 * Z-отчёт, а в настройках — ещё и пины по умолчанию. Ни одно из этого
 * на экран попасть не должно, и проверяется это здесь.
 */
class MoneyOfdSummaryTest {

    private val texts = moneyTexts(Language.Ru).kkm

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
                  "kkm": {"fnsKkmId": "FNS-2000043", "serialNumber": "SN-2000043", "kkmId": "2000043"},
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
        val summary = parseOfdInfo(answer, "ru")

        assertEquals("FNS-2000043", summary.kgdNumber)
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
        assertEquals("Пәрмен сәтті орындалды.", parseOfdInfo(answer, "kk").answer)
        assertEquals("Алматы қаласы", parseOfdInfo(answer, "kk").address)
        assertEquals("Command executed successfully.", parseOfdInfo(answer, "en").answer)
    }

    @Test
    fun `протокольный пакет и токен на экран не попадают`() {
        val shown = parseOfdInfo(answer, "ru").rows(texts).joinToString(" ") { "${it.first} ${it.second}" }

        assertFalse(shown.contains("3295187247"), "на экране оказался токен: $shown")
        assertFalse(shown.contains("-94"), "на экране оказался двоичный пакет: $shown")
        assertFalse(shown.contains("reqNum"), "на экране оказался заголовок пакета: $shown")
    }

    @Test
    fun `неразобранный ответ — пустая сводка, а не падение`() {
        assertTrue(parseOfdInfo("сломалось", "ru").rows(texts).isEmpty())
        assertTrue(parseOfdInfo("", "ru").rows(texts).isEmpty())
        assertNull(parseOfdInfo("{}", "ru").organization)
    }

    @Test
    fun `об узле показывается режим, протокол и хранилище`() {
        val body = """
            {"mode":"DESKTOP","storage":{"engine":"SQLITE","jdbcUrl":"jdbc:sqlite:data/core.db"},
             "ofdProtocolVersion":"204","ofdTimeoutSeconds":15,
             "defaultAdminPin":"4821","defaultCashierPin":"4821"}
        """.trimIndent()

        val facts = parseNodeSettings(body)

        assertEquals("DESKTOP", facts.mode)
        assertEquals("204", facts.protocol)
        assertEquals("15", facts.timeoutSeconds)
        assertEquals("SQLITE", facts.storage)
    }

    @Test
    fun `пины по умолчанию и путь к базе на экран не попадают`() {
        val body = """
            {"mode":"DESKTOP","storage":{"engine":"SQLITE","jdbcUrl":"jdbc:sqlite:data/core.db"},
             "ofdProtocolVersion":"204","ofdTimeoutSeconds":15,
             "defaultAdminPin":"4821","defaultCashierPin":"4821"}
        """.trimIndent()

        val shown = parseNodeSettings(body).rows(texts).joinToString(" ") { "${it.first} ${it.second}" }

        assertFalse(shown.contains("4821"), "на экране оказался пин: $shown")
        assertFalse(shown.contains("jdbc"), "на экране оказался путь к базе: $shown")
    }
}
