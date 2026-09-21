package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.SoldItem
import kz.mybrain.superkassa.desktop.ui.cash.CashScreen
import kz.mybrain.superkassa.desktop.ui.returns.ReturnsScreen
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Денежный ящик и возврат: обычный ход и отказные случаи.
 *
 * Снимки — `/tmp/kassa-cash-*.png` и `/tmp/kassa-return-*.png`. Смотреть
 * надо на строку под полем суммы и под кнопкой: по ней кассир понимает,
 * почему действие недоступно. Закрытая смена и нехватка денег в ящике поле
 * красным не красят — введено верно, мешает состояние кассы.
 *
 * Набор цифр сцене недоступен: поле принимает знаки только от настоящей
 * клавиатуры окна. Поэтому состояния, которые зависят от набранного,
 * проверены правилами в [MoneyCashRulesTest], а здесь сняты те, что
 * зависят от состояния кассы.
 */
class KassaMoneyLookTest {

    private fun cashDocument(no: Long, type: String, tiyn: Long) = Document(
        id = "cash-$no",
        docNo = no,
        docType = type,
        ofdStatus = "SENT",
        totalAmount = tiyn,
        createdAt = System.currentTimeMillis() - no * 600_000
    )

    private fun sold(no: Long, tiyn: Long) = Document(
        id = "sale-$no",
        docNo = no,
        docType = "SALE",
        ofdStatus = "SENT",
        fiscalSign = "38%06d".format(no),
        totalAmount = tiyn,
        createdAt = System.currentTimeMillis() - no * 900_000,
        shiftNo = 7
    )

    @Test
    fun `денежный ящик собирается во всех состояниях и они различимы`() {
        val frames = mapOf(
            "filled" to KassaScene.shot("cash-recent") {
                CashScreen(
                    KassaScene.session(
                        "cash-filled",
                        shift = KassaScene.openShift(),
                        journal = listOf(cashDocument(1, "CASH_IN", 500_000), cashDocument(2, "CASH_OUT", 150_000))
                    )
                )
            },
            "empty" to KassaScene.shot("cash-recent-empty") {
                CashScreen(KassaScene.session("cash-empty", shift = KassaScene.openShift()))
            },
            "shift-closed" to KassaScene.shot("cash-shift-closed") {
                CashScreen(KassaScene.session("cash-closed"))
            },
            "unknown-balance" to KassaScene.shot("cash-balance-unknown") {
                CashScreen(
                    KassaScene.session(
                        "cash-unknown",
                        shift = KassaScene.openShift(),
                        cashInDrawerTiyn = null,
                        available = false
                    )
                )
            },
            "blocked" to KassaScene.shot("cash-kkm-blocked") {
                CashScreen(
                    KassaScene.session(
                        "cash-blocked",
                        kkm = KassaScene.kkm(state = "BLOCKED"),
                        shift = KassaScene.openShift()
                    )
                )
            }
        )

        frames.forEach { (name, frame) -> assertTrue(frame.isNotEmpty(), "пустой кадр: $name") }
        assertTrue(
            frames.values.map { it.toList() }.distinct().size == frames.size,
            "состояния денежного ящика неотличимы друг от друга"
        )
    }

    @Test
    fun `возврат собирается во всех состояниях и они различимы`() {
        val frames = mapOf(
            "shift-closed" to KassaScene.shot("return-shift-closed") {
                ReturnsScreen(KassaScene.session("ret-closed", journal = listOf(sold(41, 1_137_250))))
            },
            "no-basis" to KassaScene.shot("return-no-basis") {
                ReturnsScreen(KassaScene.session("ret-empty", shift = KassaScene.openShift()))
            },
            "basis-list" to KassaScene.shot("return-basis-list") {
                ReturnsScreen(
                    KassaScene.session(
                        "ret-list",
                        shift = KassaScene.openShift(),
                        journal = listOf(sold(41, 1_137_250), sold(42, 69_000), sold(43, 499_000))
                    )
                )
            },
            "node-silent" to KassaScene.shot("return-node-silent") {
                ReturnsScreen(
                    KassaScene.session("ret-silent", shift = KassaScene.openShift(), available = false)
                )
            }
        )

        frames.forEach { (name, frame) -> assertTrue(frame.isNotEmpty(), "пустой кадр: $name") }
        // Молчащий узел выглядит так же, как день без продаж: экран не знает,
        // отказал узел или продаж в самом деле не было. Слова поэтому говорят
        // только о том, что узел не отдал основания, и не обещают кассиру,
        // что продаж в этот день не случилось.
        assertTrue(
            listOf(frames.getValue("shift-closed"), frames.getValue("no-basis"), frames.getValue("basis-list"))
                .map { it.toList() }.distinct().size == 3,
            "состояния возврата неотличимы друг от друга"
        )
    }

    /**
     * Панель возврата с выбранным чеком-основанием.
     *
     * Выбор делается нажатием по строке списка: панель до выбора пуста,
     * и всё, ради чего экран открыт, появляется только после него.
     */
    @Test
    fun `панель возврата с выбранным чеком показывает сумму и позиции`() {
        val session = KassaScene.session(
            "ret-chosen",
            shift = KassaScene.openShift(),
            journal = listOf(sold(41, 1_137_250)),
            sold = ITEMS
        )
        RenderProbe(
            width = KassaScene.WIDE,
            height = KassaScene.TALL,
            content = { ReturnsScreen(session) }
        ).use { probe ->
            repeat(SETTLE) { probe.frame() }
            val before = probe.frame()
            probe.click(androidx.compose.ui.geometry.Offset(BASIS_X, BASIS_Y))
            repeat(SETTLE) { probe.frame() }
            val after = probe.frame()
            java.io.File("/tmp/kassa-return-basis-chosen.png").writeBytes(after)

            assertTrue(!after.contentEquals(before), "чек-основание не выбирается нажатием")
        }
    }

    private companion object {
        const val SETTLE = 40

        /** Первая строка списка чеков-оснований: по ней и щёлкаем. */
        const val BASIS_X = 300f
        const val BASIS_Y = 230f

        val ITEMS = listOf(
            SoldItem(
                name = "Баранина на косточке, охлаждённая",
                price = BigDecimal("3450.00"),
                quantityThousandths = 1_450,
                sum = BigDecimal("5002.50"),
                vatGroup = "VAT_16",
                measureUnitCode = "166"
            ),
            SoldItem(
                name = "Коньяк «Казахстан» 0,5 л",
                price = BigDecimal("4990.00"),
                quantityThousandths = 1_000,
                sum = BigDecimal("4990.00"),
                vatGroup = "VAT_16",
                measureUnitCode = "796"
            )
        )
    }
}
