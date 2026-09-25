package kz.mybrain.superkassa.presentation.cabinet

import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kz.mybrain.superkassa.presentation.cabinet.register.tokenAllowed
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
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

    private val texts = textsOf(Language.Ru).cabinet

    private fun register(status: String) = CabinetRegister(id = "id", kkmId = 1, status = status)

    @Test
    fun `известный отказ переводится, а код остаётся для поддержки`() {
        val message = cabinetMessage(
            CabinetProblem.Refused("CASH_REGISTER_STATUS", "Token is issued only for a registered cash register"),
            texts
        )
        val refusal = assertIs<Message.Refusal>(message)
        assertEquals(texts.register.tokenOnlyRegistered, refusal.text)
        assertEquals("CASH_REGISTER_STATUS", refusal.code)
    }

    /**
     * Молчание NCALayer и его отсутствие — разные строки.
     *
     * «NCALayer не отвечает, запустите его» при работающем NCALayer —
     * ложь: владелец подписал в его окне, а приложение через три минуты
     * предложило его запустить. Молчание называется молчанием и говорит,
     * где искать окно подписи.
     */
    @Test
    fun `молчание NCALayer не предлагает его запустить`() {
        val silent = cabinetMessage(CabinetProblem.SignDeclined(Signer.NO_ANSWER), texts)
        val absent = cabinetMessage(CabinetProblem.NoNcaLayer, texts)

        val words = assertIs<Message.Refusal>(silent).text
        assertTrue(words.contains(texts.hints.signNoAnswer), words)
        assertFalse(words.contains(texts.refusal.noNcaLayer), "молчание выдано за отсутствие: $words")
        assertEquals(texts.refusal.noNcaLayer, assertIs<Message.Refusal>(absent).text)
    }

    /** Закрытое окно подписи названо своими словами, а не кодом. */
    @Test
    fun `закрытое окно подписи названо словами владельца`() {
        val message = cabinetMessage(CabinetProblem.SignDeclined(Signer.WINDOW_CLOSED), texts)
        val words = assertIs<Message.Refusal>(message).text

        assertTrue(words.contains(texts.refusal.signWindowClosed), words)
        assertFalse(words.contains("WINDOW_CLOSED"), "код вместо слов: $words")
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
        assertEquals(texts.refusal.unreachable, refusal.text)
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
        assertEquals(texts.applications.shiftOpenTitle, assertIs<Message.Refusal>(message).text)
    }

    @Test
    fun `истёкший доступ и отказ подписи говорят по-русски`() {
        assertEquals(
            texts.refusal.sessionExpired,
            assertIs<Message.Refusal>(cabinetMessage(CabinetProblem.SessionExpired, texts)).text
        )
        val refusal = assertIs<Message.Refusal>(cabinetMessage(CabinetProblem.NoNcaLayer, texts))
        assertEquals(texts.refusal.noNcaLayer, refusal.text)
    }

    @Test
    fun `токен просят только у кассы на учёте`() {
        assertTrue(tokenAllowed(register("REGISTERED")))
        assertTrue(tokenAllowed(register("REGISTERED_REREGISTRATION_SUCCESS")))
        assertFalse(tokenAllowed(register("DRAFT")), "по черновику кабинет ответит отказом")
        assertFalse(tokenAllowed(register("REGISTRATION_IN_ISNA_PROCESS")))
        assertFalse(tokenAllowed(register("DEREGISTERED")))
    }

    /**
     * Отказы подписи на Android — словами владельца, без NCALayer.
     *
     * Отмена у кассы прежде читалась бы «Подпись отменена в NCALayer» —
     * на планшете, где NCALayer нет и не бывает.
     */
    @Test
    fun `отказы eGov mobile и отмена у кассы названы без NCALayer`() {
        val words = listOf(Signer.CANCELLED, Signer.EGOV_UNREACHABLE, Signer.EGOV_EXPIRED)
            .map { assertIs<Message.Refusal>(cabinetMessage(CabinetProblem.SignDeclined(it), texts)).text }
        assertTrue(words[0].contains(texts.eds.cancelled), words[0])
        assertTrue(words[1].contains(texts.eds.egovUnreachable), words[1])
        assertTrue(words[2].contains(texts.eds.egovExpired), words[2])
        assertTrue(words.none { it.contains("NCALayer") }, words.toString())
    }
}
