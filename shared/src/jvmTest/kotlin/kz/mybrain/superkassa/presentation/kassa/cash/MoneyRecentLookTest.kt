package kz.mybrain.superkassa.presentation.kassa.cash

import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.domain.kassa.model.entry.amount
import kz.mybrain.superkassa.kassa.CoreScene
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Список движений наличных: пустые сутки и неотвеченный запрос.
 *
 * Кассир сводит ящик по этому списку при закрытии смены. «Внесений
 * и изъятий не было» над молчащим узлом — утверждение о деньгах, которого
 * узел не делал, и расхождение в ящике кассир увидит уже после Z-отчёта.
 *
 * Снимки — `/tmp/kassa-cash-recent-*.png`.
 */
class MoneyRecentLookTest {

    private fun cashDocument(no: Long, type: String, tiyn: Long) =
        CoreScene.document("cash-$no", type = type, amount = tiyn, status = "SENT")
            .copy(docNo = no, createdAt = System.currentTimeMillis() - no * 600_000)

    /** Ящик с открытой сменой; что касса сказала о сутках — снаружи. */
    private fun drawer(read: Boolean, recent: List<FiscalDocumentResponse> = emptyList()) = CashUiState(
        kkm = CoreScene.kkm(),
        signedIn = true,
        shiftOpen = true,
        cashInDrawer = 125_000,
        recent = recent,
        recentRead = read,
        recentLoading = false
    )

    @Test
    fun `молчание кассы не показывается сутками без движений`() {
        val empty = KassaScene.shot("cash-recent-empty-day") {
            CashContent(drawer(read = true))
        }
        val unread = KassaScene.shot("cash-recent-unread") {
            CashContent(drawer(read = false))
        }

        assertTrue(
            !empty.contentEquals(unread),
            "сутки без движений и молчание кассы показаны одинаково: кассир сводит ящик по выдуманной пустоте"
        )
    }

    /** Прочитанные движения остаются на месте: отказной вид не подменяет список. */
    @Test
    fun `прочитанные движения показываются списком`() {
        val filled = KassaScene.shot("cash-recent-filled") {
            CashContent(drawer(true, listOf(cashDocument(1, "CASH_IN", 500_000), cashDocument(2, "CASH_OUT", 150_000))))
        }
        val empty = KassaScene.shot("cash-recent-empty-again") {
            CashContent(drawer(read = true))
        }

        assertTrue(!filled.contentEquals(empty), "список движений не отличается от пустых суток")
    }
}
