package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.ui.cabinet.cabinetHead
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Шапка окна называет то место, где владелец сейчас находится.
 *
 * Навигация в приложении одна и живёт в шапке: по этому же признаку она
 * решает, куда ведёт стрелка возврата. Поэтому признак проверяется прямо,
 * а не через разметку — от него зависит, не уведёт ли возврат из кабинета
 * целиком там, где владелец ждал возврата к карточке кассы.
 */
class CabinetHeadTest {

    private val texts = cabinetTexts(Language.Ru)
    private val documentsTitle = journalTexts(Language.Ru).history.registerDocuments

    private fun head(register: CabinetRegister?, company: String? = "ТОО «Пример»", owner: String? = "Владелец") =
        cabinetHead(register, company, owner, texts, documentsTitle)

    @Test
    fun `в кабинете шапка называет компанию и владельца`() {
        val head = head(register = null)
        assertEquals("ТОО «Пример»", head.title)
        assertEquals("Владелец", head.subtitle)
        assertFalse(head.inDocuments)
    }

    @Test
    fun `до входа по ЭЦП шапка называет сам кабинет`() {
        val head = head(register = null, company = null, owner = null)
        assertEquals(texts.title, head.title)
        assertNull(head.subtitle)
        assertFalse(head.inDocuments)
    }

    @Test
    fun `пустое название компании не выходит на экран пустой строкой`() {
        assertEquals(texts.title, head(register = null, company = "   ").title)
    }

    @Test
    fun `открытые документы кассы называют себя и кассу`() {
        val head = head(register(name = "Касса у входа"))
        assertEquals(documentsTitle, head.title)
        assertEquals("Касса у входа", head.subtitle)
        assertTrue(head.inDocuments)
    }

    @Test
    fun `безымянная касса называется номером КГД`() {
        assertEquals("000000000007", head(register(name = null)).subtitle)
    }

    private fun register(name: String?) = CabinetRegister(
        id = "r1",
        kkmId = 1,
        internalName = name,
        status = "REGISTERED",
        registrationNumber = "000000000007"
    )
}
