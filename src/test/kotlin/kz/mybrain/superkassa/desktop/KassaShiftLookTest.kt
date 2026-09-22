package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.ui.dashboard.DashboardScreen
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Главный экран смены во всех своих состояниях.
 *
 * Снимки — `/tmp/kassa-shift-*.png`. Смотреть в них надо на одно: понятно
 * ли кассиру, что делать сейчас. Заблокированная касса кнопок не получает,
 * и вместо них обязана стоять строка о том, почему; смена, которой узел
 * не назвал, не повод предлагать её открыть.
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
        // Сеансы создаются до сцены: вызов внутри её содержимого повторяется
        // на каждой перерисовке, и снимок доставался каждый раз новому сеансу.
        val noShift = KassaScene.session("shift-none")
        val open = KassaScene.session(
            "shift-open",
            shift = KassaScene.openShift(),
            documents = (1L..6L).map { sale(it, it * 120_000) }
        )
        // Документы те же, что у обычной открытой смены: разниться эти два
        // случая обязаны сроком смены, а не составом списка. С разными
        // документами кадры отличались друг от друга и тогда, когда экран
        // о вторых сутках смены не говорил ни слова.
        val dayLong = KassaScene.session(
            "shift-long",
            shift = KassaScene.openShift(openedAt = System.currentTimeMillis() - DAY - HOUR),
            documents = (1L..6L).map { sale(it, it * 120_000) }
        )
        val blocked = KassaScene.session(
            "shift-blocked",
            kkm = KassaScene.kkm(state = "BLOCKED"),
            shift = KassaScene.openShift()
        )
        val silent = KassaScene.session(
            "shift-silent",
            available = false,
            shiftAnswered = false,
            cashInDrawerTiyn = null
        )
        val refused = KassaScene.session(
            "shift-refused",
            shift = KassaScene.openShift(),
            documents = listOf(sale(11, 340_000), sale(12, 99_000, refused = true))
        )
        val cashier = KassaScene.session("shift-cashier", admin = false)

        val frames = mapOf(
            "no-shift" to KassaScene.shot("shift-none") { DashboardScreen(noShift) },
            "open" to KassaScene.shot("shift-open") { DashboardScreen(open) },
            "day-long" to KassaScene.shot("shift-day-long") { DashboardScreen(dayLong) },
            "blocked" to KassaScene.shot("shift-kkm-blocked") { DashboardScreen(blocked) },
            "node-silent" to KassaScene.shot("shift-node-silent") { DashboardScreen(silent) },
            "refused" to KassaScene.shot("shift-refused-document") { DashboardScreen(refused) },
            "cashier" to KassaScene.shot("shift-closed-cashier") { DashboardScreen(cashier) }
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
