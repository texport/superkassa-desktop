package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.OrgInfo
import kz.mybrain.superkassa.desktop.ui.login.kkmDetail
import kz.mybrain.superkassa.desktop.ui.login.kkmNumber
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Строка кассы на экране входа.
 *
 * Кассир узнаёт свою кассу по названию, а сверяет её по регистрационному
 * номеру КГД. Прежде в строке стоял один номер без подписи, и названная
 * по-своему касса читалась как чужая.
 */
class LoginKkmNameTest {

    private val texts = stringsOf(Language.Ru).login

    private val kkm = Kkm(
        kkmId = "1f2e",
        kkmKgdId = "000000000042",
        factoryNumber = "SK-77",
        ofdServiceInfo = OrgInfo(orgTitle = "ТОО «Пример»", orgAddress = "Алматы, Абая 1")
    )

    @Test
    fun `номер КГД в подписи назван словом`() {
        assertEquals("РНМ 000000000042", kkmNumber(kkm, texts))
    }

    @Test
    fun `подпись строки несёт номер, владельца, адрес и заводской`() {
        assertEquals(
            "РНМ 000000000042 · ТОО «Пример» · Алматы, Абая 1 · Заводской SK-77",
            kkmDetail(kkm, texts)
        )
    }

    @Test
    fun `у кассы без учёта в КГД номера в подписи нет`() {
        val fresh = kkm.copy(kkmKgdId = null)
        assertNull(kkmNumber(fresh, texts))
        assertTrue(!kkmDetail(fresh, texts).contains(texts.registrationNumber))
    }

    @Test
    fun `пустой номер не выходит на экран подписью без числа`() {
        assertNull(kkmNumber(kkm.copy(kkmKgdId = "  "), texts))
    }

    @Test
    fun `название кассы остаётся отдельно от подписи`() {
        // Заголовок строки — название рабочего места, подпись — учётные
        // сведения: одно не подменяет другое.
        assertTrue(!kkmDetail(kkm, texts).contains("Касса у входа"))
    }

    @Test
    fun `подпись номера заполнена на каждом языке`() {
        Language.entries.forEach { language ->
            val label = stringsOf(language).login.registrationNumber
            assertTrue(label.isNotBlank(), "$language: пустая подпись номера")
        }
    }
}
