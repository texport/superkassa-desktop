package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.app.CabinetProblem
import kz.mybrain.superkassa.desktop.app.Message
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.ui.cabinet.cabinetMessage
import kz.mybrain.superkassa.desktop.ui.cabinet.tokenAllowed
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Отказы кабинета глазами владельца.
 *
 * Кабинет отвечает кодом и английским пояснением для поддержки. Владельцу
 * нужно то же самое его словами и в том же месте, где он читает отказы
 * кассы, — во всплывающей строке внизу окна.
 */
class CabinetMessageTest {

    private val texts = cabinetTexts(Language.Ru)

    private fun register(status: String) = CabinetRegister(id = "id", kkmId = 1, status = status)

    @Test
    fun `известный отказ переводится, а код остаётся для поддержки`() {
        val message = cabinetMessage(
            CabinetProblem.Refused("CASH_REGISTER_STATUS", "Token is issued only for a registered cash register"),
            texts
        )
        val refusal = assertIs<Message.Refusal>(message)
        assertEquals(texts.tokenOnlyRegistered, refusal.text)
        assertEquals("CASH_REGISTER_STATUS", refusal.code)
    }

    @Test
    fun `незнакомый код доходит с пояснением сервера, а не молчанием`() {
        val message = cabinetMessage(CabinetProblem.Refused("SOMETHING_NEW", "Server said this"), texts)
        assertEquals("Server said this", assertIs<Message.Refusal>(message).text)
    }

    /**
     * Молчание кабинета названо молчанием кабинета.
     *
     * Прежде оно доходило общей строкой о недоступной службе, а та
     * называет узел кассы: владелец читал «Узел кассы недоступен ·
     * Кабинет БФД» — про узел, который в это время пробивает чеки.
     * Строка теперь своя, кабинетная; код остаётся отдельным, и по нему
     * поддержка отличает молчание службы от отказа по существу.
     */
    @Test
    fun `недоступный кабинет говорит о кабинете, а не об узле кассы`() {
        val message = cabinetMessage(CabinetProblem.Unreachable("Connection refused"), texts)
        val refusal = assertIs<Message.Refusal>(message)
        assertEquals(texts.unreachable, refusal.text)
        assertEquals("CABINET_UNREACHABLE", refusal.code)
    }

    /**
     * Открытая смена — отказ, который приложение исправляет само.
     *
     * Кабинет отвечает `SHIFT_IS_OPEN` и английским пояснением, а рядом
     * с этой строкой стоит кнопка закрытия смены по-русски: причина
     * обязана говорить на том же языке, что и кнопка под ней.
     */
    @Test
    fun `открытая смена названа словами владельца`() {
        val message = cabinetMessage(CabinetProblem.Refused("SHIFT_IS_OPEN", "Shift is open"), texts)
        assertEquals(texts.shiftOpenTitle, assertIs<Message.Refusal>(message).text)
    }

    @Test
    fun `истёкший доступ и отказ подписи говорят по-русски`() {
        assertEquals(texts.sessionExpired, assertIs<Message.Refusal>(cabinetMessage(CabinetProblem.SessionExpired, texts)).text)
        assertEquals(texts.noNcaLayer, assertIs<Message.Refusal>(cabinetMessage(CabinetProblem.NoNcaLayer, texts)).text)
    }

    @Test
    fun `токен просят только у кассы на учёте`() {
        assertTrue(tokenAllowed(register("REGISTERED")))
        assertTrue(tokenAllowed(register("REGISTERED_REREGISTRATION_SUCCESS")))
        assertFalse(tokenAllowed(register("DRAFT")), "по черновику кабинет ответит отказом")
        assertFalse(tokenAllowed(register("REGISTRATION_IN_ISNA_PROCESS")))
        assertFalse(tokenAllowed(register("DEREGISTERED")))
    }
}
