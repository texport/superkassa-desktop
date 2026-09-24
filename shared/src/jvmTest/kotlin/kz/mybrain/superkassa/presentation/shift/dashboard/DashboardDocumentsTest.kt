package kz.mybrain.superkassa.presentation.shift.dashboard

import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.DashboardScene
import kz.mybrain.superkassa.kassa.state
import kz.mybrain.superkassa.kassa.unread
import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * Число документов смены на главном экране — только то, что назвала касса.
 *
 * У кассы, снятой с учёта, смена открыта, а на её документы касса отвечает
 * KKM_BLOCKED. Плитка «Документов за смену» показывала при этом ноль,
 * а на месте списка стояло «Документов пока нет» с обещанием, что первый
 * чек вот-вот появится: кассир читал это как пустую смену и решал по ней,
 * можно ли снимать Z-отчёт.
 */
class DashboardDocumentsTest {

    /**
     * Смена без единого чека и смена, документов которой не видно, —
     * разные картинки, а не одна на оба случая.
     */
    @Test
    fun `пустая смена и непрочитанные документы выглядят по-разному`() {
        val empty = DashboardScene.state(shift = CoreScene.openShift())
        val unread = with(DashboardScene) { state(shift = CoreScene.openShift()).unread() }

        val emptyFrame = KassaScene.shot("dash-shift-empty") { DashboardContent(empty) }
        val unreadFrame = KassaScene.shot("dash-shift-unread") { DashboardContent(unread) }

        assertFalse(
            emptyFrame.contentEquals(unreadFrame),
            "смена без чеков и смена, документы которой касса не отдала, показаны одинаково"
        )
    }
}
