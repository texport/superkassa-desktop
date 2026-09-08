package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.ui.cabinet.ActionKind
import kz.mybrain.superkassa.desktop.ui.cabinet.availableActions
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

    @Test
    fun `причина названа по существу, а не одной строкой на оба случая`() {
        val texts = cabinetTexts(Language.Ru)
        assertEquals(texts.applicationInFlight, noActionsReason(register("REGISTRATION_IN_ISNA_PROCESS"), texts))
        assertEquals(texts.noApplications, noActionsReason(register("DEREGISTERED"), texts))
    }
}
