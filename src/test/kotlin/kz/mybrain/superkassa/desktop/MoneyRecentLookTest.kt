package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.ui.cash.CashScreen
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

    private fun cashDocument(no: Long, type: String, tiyn: Long) = Document(
        id = "cash-$no",
        docNo = no,
        docType = type,
        ofdStatus = "SENT",
        totalAmount = tiyn,
        createdAt = System.currentTimeMillis() - no * 600_000
    )

    /**
     * Узел не ответил — и экран обязан сказать именно это.
     *
     * Сравниваются два кадра одной и той же кассы: в первом узел отдал
     * пустые сутки, во втором не ответил вовсе. До правки оба кадра были
     * одной картинкой — «За сутки внесений и изъятий не было».
     */
    @Test
    fun `молчание узла не показывается сутками без движений`() {
        val empty = KassaScene.shot("cash-recent-empty-day") {
            CashScreen(KassaScene.session("cash-empty-day", shift = KassaScene.openShift()))
        }
        val unread = KassaScene.shot("cash-recent-unread") {
            CashScreen(
                KassaScene.session(
                    "cash-unread",
                    shift = KassaScene.openShift(),
                    journalAnswered = false
                )
            )
        }

        assertTrue(
            !empty.contentEquals(unread),
            "сутки без движений и молчание узла показаны одинаково: кассир сводит ящик по выдуманной пустоте"
        )
    }

    /** Прочитанные движения остаются на месте: отказной вид не подменяет список. */
    @Test
    fun `прочитанные движения показываются списком`() {
        val filled = KassaScene.shot("cash-recent-filled") {
            CashScreen(
                KassaScene.session(
                    "cash-filled-day",
                    shift = KassaScene.openShift(),
                    journal = listOf(cashDocument(1, "CASH_IN", 500_000), cashDocument(2, "CASH_OUT", 150_000))
                )
            )
        }
        val empty = KassaScene.shot("cash-recent-empty-again") {
            CashScreen(KassaScene.session("cash-empty-again", shift = KassaScene.openShift()))
        }

        assertTrue(!filled.contentEquals(empty), "список движений не отличается от пустых суток")
    }
}
