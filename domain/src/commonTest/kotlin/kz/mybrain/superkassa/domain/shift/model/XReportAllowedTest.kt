package kz.mybrain.superkassa.domain.shift.model

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Когда кассе предлагать X-отчёт.
 *
 * БФД открывает смену первым документом с суммой; до него X-отчёт
 * возвращался отказом с кодом 13.
 */
class XReportAllowedTest {

    private fun document(type: String, status: String = "SENT", code: Int? = null) = FiscalDocumentResponse(
        id = type,
        cashboxId = "kkm-1",
        shiftId = "shift-1",
        docType = type,
        docNo = null,
        shiftNo = 1,
        createdAt = 0,
        totalAmount = null,
        currency = "KZT",
        fiscalSign = null,
        autonomousSign = null,
        isAutonomous = false,
        ofdStatus = status,
        ofdErrorCode = code,
        deliveredAt = null
    )

    @Test
    fun `пустая смена — X-отчёта нет`() {
        assertFalse(xReportAllowed(emptyList()))
    }

    @Test
    fun `открытие смены и прежние отчёты смену у БФД не открывают`() {
        val opened = listOf(document("SHIFT_OPEN", "INTERNAL"), document("X_REPORT", "FAILED", 13))
        assertFalse(xReportAllowed(opened))
    }

    @Test
    fun `внесение открывает смену для X-отчёта`() {
        assertTrue(xReportAllowed(listOf(document("SHIFT_OPEN", "INTERNAL"), document("CASH_IN"))))
    }

    @Test
    fun `чек без связи — фискальный и уйдёт первым`() {
        assertTrue(xReportAllowed(listOf(document("SALE", "OFFLINE_QUEUED"))))
    }

    @Test
    fun `отвергнутый БФД чек смену не открыл`() {
        assertFalse(xReportAllowed(listOf(document("SALE", "FAILED", 5))))
    }
}
