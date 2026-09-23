package kz.mybrain.superkassa

import kz.mybrain.superkassa.data.node.Document
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.DashboardScene
import kz.mybrain.superkassa.presentation.dashboard.DashboardContent
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Главный экран смены во всех своих состояниях.
 *
 * Снимки — `/tmp/kassa-shift-*.png`. Смотреть в них надо на одно: понятно
 * ли кассиру, что делать сейчас. Заблокированная касса кнопок не получает,
 * и вместо них обязана стоять строка о том, почему; смена, которой касса
 * не назвала, не повод предлагать её открыть.
 */
class KassaShiftLookTest {

    private fun sale(no: Long, tiyn: Long, refused: Boolean = false) = Document(
        id = "doc-$no",
        docNo = no,
        docType = "SALE",
        ofdStatus = if (refused) "FAILED" else "SENT",
        ofdErrorCode = if (refused) 4 else null,
        fiscalSign = "38%06d".format(no),
        totalAmount = tiyn,
        createdAt = System.currentTimeMillis() - no * MINUTE,
        shiftNo = 7
    )

    @Test
    fun `главный экран собирается во всех состояниях смены и они различимы`() {
        // Состояния собираются до сцены: вызов внутри её содержимого
        // повторяется на каждой перерисовке.
        val noShift = DashboardScene.state()
        val open = DashboardScene.state(
            shift = CoreScene.openShift(),
            documents = (1L..6L).map { sale(it, it * 120_000) }
        )
        // Документы те же, что у обычной открытой смены: разниться эти два
        // случая обязаны сроком смены, а не составом списка.
        val dayLong = DashboardScene.state(
            shift = CoreScene.openShift(openedAt = System.currentTimeMillis() - DAY - HOUR),
            documents = (1L..6L).map { sale(it, it * 120_000) }
        )
        val blocked = DashboardScene.state(kkm = CoreScene.kkm(state = "BLOCKED"), shift = CoreScene.openShift())
        val silent = with(DashboardScene) { state(cash = null).unknown() }
        val refused = DashboardScene.state(
            shift = CoreScene.openShift(),
            documents = listOf(sale(11, 340_000), sale(12, 99_000, refused = true))
        )
        val cashier = DashboardScene.state(admin = false)

        val frames = mapOf(
            "no-shift" to KassaScene.shot("shift-none") { DashboardContent(noShift) },
            "open" to KassaScene.shot("shift-open") { DashboardContent(open) },
            "day-long" to KassaScene.shot("shift-day-long") { DashboardContent(dayLong) },
            "blocked" to KassaScene.shot("shift-kkm-blocked") { DashboardContent(blocked) },
            "kassa-silent" to KassaScene.shot("shift-kassa-silent") { DashboardContent(silent) },
            "refused" to KassaScene.shot("shift-refused-document") { DashboardContent(refused) },
            "cashier" to KassaScene.shot("shift-closed-cashier") { DashboardContent(cashier) }
        )

        frames.forEach { (name, frame) -> assertTrue(frame.isNotEmpty(), "пустой кадр: $name") }
        assertTrue(
            frames.values.map { it.toList() }.distinct().size == frames.size,
            "состояния главного экрана неотличимы друг от друга"
        )
    }

    private companion object {
        const val MINUTE = 60_000L
        const val HOUR = 60 * MINUTE
        const val DAY = 24 * HOUR
    }
}
