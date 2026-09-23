package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.OrgInfo
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Организация кассы: отсутствие сведений остаётся отсутствием.
 *
 * Модель подставляла вместо пустого поля русскую надпись, и та шла двумя
 * путями сразу: на казахский и английский экран по-русски и в строку,
 * по которой кассир ищет свою кассу. Набор «Орг» отбирал все кассы,
 * у которых сведений об организации нет вовсе.
 */
class KkmOrgTitleTest {

    private fun kkm(org: String? = null) = Kkm(
        kkmId = "kkm-1",
        kkmKgdId = "000000200042",
        factoryNumber = "SK-000042",
        ofdServiceInfo = org?.let { OrgInfo(orgTitle = it, orgAddress = "Алматы, Абая 150") }
    )

    @Test
    fun `у кассы без сведений об организации названия организации нет`() {
        assertNull(kkm().orgTitle, "отсутствие сведений — это отсутствие значения, а не надпись")
        assertNull(kkm(org = "   ").orgTitle, "пробелы от ОФД — те же несведения")
        assertEquals("ТОО «Пример»", kkm(org = "ТОО «Пример»").orgTitle)
    }

    @Test
    fun `поиск по организации не отбирает кассы без организации`() {
        assertFalse(
            kkm().matches("Орг"),
            "касса без сведений об организации отобралась набором «Орг»: искалась подставленная надпись"
        )
        assertTrue(kkm(org = "ТОО «Органика»").matches("Орг"), "своя организация обязана находиться набором")
    }

    @Test
    fun `отсутствие организации названо на всех трёх языках`() {
        val words = Language.entries.associateWith { stringsOf(it).settings.orgUnknown }

        words.forEach { (language, text) ->
            assertTrue(text.isNotBlank(), "надписи об отсутствии организации нет на языке $language")
        }
        assertEquals(
            Language.entries.size,
            words.values.distinct().size,
            "надпись повторяется дословно: значит один из языков остался непереведённым"
        )
    }
}
