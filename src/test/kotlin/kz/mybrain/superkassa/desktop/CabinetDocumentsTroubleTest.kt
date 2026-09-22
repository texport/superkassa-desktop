package kz.mybrain.superkassa.desktop

import io.ktor.http.HttpStatusCode
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetDocumentsScreen
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Документы кассы, когда кабинет их не отдал.
 *
 * Экран документов — это то, что БФД приняла от кассы: по нему владелец
 * судит, дошли ли чеки. Отказ кабинета он показывал теми же словами, что
 * и кассу без единого документа: «Документов нет — здесь появится то,
 * что БФД приняла от этой кассы». О кассе, пробившей тысячу чеков, это
 * говорит владельцу, что чеки потеряны.
 */
class CabinetDocumentsTroubleTest {

    private fun register() = CabinetRegister(id = "r-1", kkmId = 5_000_021, status = "REGISTERED")

    private fun documents(name: String, reply: (String) -> CabinetReply): ByteArray {
        val stage = CabinetStage(reply)
        return RenderProbe(WIDE, HIGH) {
            CabinetDocumentsScreen(stage.session, stage.cabinet, stage.texts, register())
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            val frame = probe.frame()
            Look.shot(name, frame)
            frame
        }
    }

    @Test
    fun `отказ кабинета не выдаётся за кассу без документов`() {
        val empty = documents("audit-cabinet-documents-empty") {
            CabinetReply(CabinetBodies.NOTHING)
        }
        val refused = documents("audit-cabinet-documents-refused") {
            refusal("INTERNAL_ERROR", "Кабинет временно недоступен", HttpStatusCode.ServiceUnavailable)
        }

        assertTrue(
            !empty.contentEquals(refused),
            "отказ кабинета показан теми же словами, что и касса без документов"
        )
    }

    private companion object {
        const val WIDE = 1180
        const val HIGH = 820
        const val SETTLE = 40
    }
}
