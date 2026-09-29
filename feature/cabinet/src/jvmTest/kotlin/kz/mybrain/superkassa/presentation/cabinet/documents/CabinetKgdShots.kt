package kz.mybrain.superkassa.presentation.cabinet.documents

import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.CabinetStage
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.StubReply
import kz.mybrain.superkassa.data.cabinet.CabinetBodies
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Состояние в КГД в журнале кассы 5005018.
 *
 * Чек, возврат и Z-отчёт приняты КГД; X-отчёт, внесение и изъятие в КГД
 * не передаются. Прежде у всех документов стояло одно «Принят».
 */
class CabinetKgdShots {
    private val docs = textsOf(Language.Ru).cabinet.documents

    private val kassa = CabinetRegister(
        id = "r-5005018",
        kkmId = 5_005_018,
        internalName = "Касса 5005018",
        status = "REGISTERED",
        registrationNumber = "000000050018"
    )

    @Test
    fun `журнал кабинета показывает доставку в КГД`() {
        val receipts = frame("kgd-5005018-receipts", null)
        val reports = frame("kgd-5005018-reports", REPORTS_X)
        val cash = frame("kgd-5005018-cash", CASH_X)

        assertEquals(2, receipts.count { it == docs.kgdAccepted }, "чек и возврат: $receipts")
        assertTrue(docs.kgdAccepted in reports && docs.kgdNotSent in reports, "Z и X: $reports")
        assertEquals(2, cash.count { it == docs.kgdNotSent }, "внесение и изъятие: $cash")
    }

    /** Надписи списка вида [at] (`null` — чеки); кадр — в `/tmp`. */
    private fun frame(name: String, at: Float?): List<String> {
        val stage = CabinetStage { path ->
            when {
                path.endsWith("/documents") -> StubReply(CabinetBodies.OVERVIEW)
                path.endsWith("/receipts/search") -> StubReply(RECEIPT_AND_RETURN)
                path.contains("/reports") -> StubReply(CabinetBodies.REPORTS)
                path.contains("/cash-movements") -> StubReply(CabinetBodies.MOVEMENTS)
                else -> StubReply(CabinetBodies.NOTHING)
            }
        }
        RenderProbe(content = {
            stage.Window { CabinetDocumentsScreen(stage.cabinet.cabinet, stage.texts, kassa) }
        }).use { probe ->
            repeat(SETTLE) { probe.frame() }
            at?.let { probe.click(Offset(it, KIND_Y)) }
            var shot = probe.frame()
            repeat(SETTLE) { shot = probe.frame() }
            File("/tmp/cabinet-$name.png").writeBytes(shot)
            return probe.nodes().map { it.text }
        }
    }

    private companion object {
        const val SETTLE = 30

        /** Ряд видов документов над списком — как в [CabinetDocumentShots]. */
        const val KIND_Y = 114f
        const val REPORTS_X = 465f
        const val CASH_X = 646f

        /** Чек и возврат, принятые КГД, — как их отдаёт кабинет. */
        const val RECEIPT_AND_RETURN = """{"page":0,"size":50,"totalElements":2,"items":[
            {"transactionId":"t-1","receiptNumber":"1","shiftNumber":1,"operationType":"SALE",
             "total":1500.00,"createdAt":"2026-09-29T09:12:00Z","sendStatus":"ACCEPTED",
             "deliveryStatus":"DELIVERED","kgdMark":"210000000101"},
            {"transactionId":"t-2","receiptNumber":"2","shiftNumber":1,"operationType":"RETURN",
             "total":500.00,"createdAt":"2026-09-29T09:20:00Z","sendStatus":"ACCEPTED",
             "deliveryStatus":"DELIVERED","kgdMark":"210000000102"}]}"""
    }
}
