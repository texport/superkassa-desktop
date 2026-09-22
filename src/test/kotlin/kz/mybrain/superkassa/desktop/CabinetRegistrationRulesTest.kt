package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.ui.cabinet.ActionKind
import kz.mybrain.superkassa.desktop.ui.cabinet.availableActions
import kz.mybrain.superkassa.desktop.ui.cabinet.editableInCabinet
import kz.mybrain.superkassa.desktop.ui.cabinet.noActionsReason
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Какое заявление в ИСНА подаётся по кассе.
 *
 * Прежде предлагались все три всегда, и кассу, стоящую на учёте, можно
 * было отправить ставить на учёт заново: ИСНА отвечала отказом через
 * минуты, и отказ приходил кодом.
 */
class CabinetRegistrationRulesTest {

    private fun register(status: String) = CabinetRegister(id = "id", kkmId = 1, status = status)

    @Test
    fun `черновик только ставится на учёт`() {
        assertEquals(setOf(ActionKind.Registration), availableActions(register("DRAFT")))
    }

    @Test
    fun `после отказа ИСНА постановка повторяется`() {
        assertEquals(
            setOf(ActionKind.Registration),
            availableActions(register("REGISTRATION_IN_ISNA_ERROR"))
        )
    }

    @Test
    fun `стоящая на учёте перерегистрируется или снимается`() {
        val expected = setOf(ActionKind.Reregistration, ActionKind.Deregistration)
        assertEquals(expected, availableActions(register("REGISTERED")))
        assertEquals(expected, availableActions(register("REGISTERED_REREGISTRATION_SUCCESS")))
    }

    @Test
    fun `пока ИСНА не ответила, не подаётся ничего`() {
        listOf(
            "REGISTRATION_IN_ISNA_PROCESS",
            "REREGISTRATION_IN_ISNA_PROCESS",
            "DEREGISTRATION_IN_ISNA_PROCESS"
        ).forEach { status ->
            assertEquals(emptySet(), availableActions(register(status)), status)
        }
    }

    @Test
    fun `по снятой с учёта не подаётся ничего`() {
        assertEquals(emptySet(), availableActions(register("DEREGISTERED")))
    }

    /**
     * Правка и удаление — пока в КГД по кассе ничего не ушло.
     *
     * Черновиком считалась любая касса без номера КГД. Номера нет и у той,
     * чьё заявление КГД сейчас рассматривает: заводской номер у неё
     * правился прямо в рассматриваемом заявлении, а кнопка удаления
     * снимала кассу, о которой уже спрошено.
     */
    @Test
    fun `касса с заявлением в работе не правится и не удаляется`() {
        assertEquals(true, editableInCabinet(register("DRAFT")), "черновик перестал правиться")
        assertEquals(
            true,
            editableInCabinet(register("REGISTRATION_IN_ISNA_ERROR")),
            "после отказа КГД заводской номер не исправить"
        )
        listOf("REGISTRATION_IN_ISNA_PROCESS", "REREGISTRATION_IN_ISNA_PROCESS").forEach { status ->
            assertEquals(false, editableInCabinet(register(status)), status)
        }
    }

    @Test
    fun `поставленная на учёт не правится`() {
        val onRecord = CabinetRegister(id = "id", kkmId = 1, status = "REGISTERED", registrationNumber = "000000000001")
        assertEquals(false, editableInCabinet(onRecord))
    }

    @Test
    fun `причина названа по существу, а не одной строкой на оба случая`() {
        val texts = cabinetTexts(Language.Ru)
        assertEquals(texts.applicationInFlight, noActionsReason(register("REGISTRATION_IN_ISNA_PROCESS"), texts))
        assertEquals(texts.noApplications, noActionsReason(register("DEREGISTERED"), texts))
    }
}
