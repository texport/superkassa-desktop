package kz.mybrain.superkassa.presentation.users.signin

import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
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

    private val texts = textsOf(Language.Ru).common.login

    private val kkm = CoreScene.kkm(id = "1f2e", kgd = "000000000042").let {
        it.copy(factoryNumber = "SK-77", ofdServiceInfo = it.ofdServiceInfo?.copy(orgAddress = "Алматы, Абая 1"))
    }

    @Test
    fun `номер КГД в подписи назван словом`() {
        assertEquals("Номер КГД 000000000042", kkmNumber(kkm, texts))
    }

    @Test
    fun `подпись строки несёт номер, владельца, адрес и заводской`() {
        assertEquals(
            "Номер КГД 000000000042 · ТОО «Пример» · Алматы, Абая 1 · Заводской SK-77",
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
            val label = textsOf(language).common.login.registrationNumber
            assertTrue(label.isNotBlank(), "$language: пустая подпись номера")
        }
    }
}
