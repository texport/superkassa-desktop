package kz.mybrain.superkassa.presentation.kassa.cash

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.domain.kassa.model.entry.amount
import kz.mybrain.superkassa.kassa.CoreScene
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Денежный ящик: обычный ход и отказные случаи.
 *
 * Снимки — `/tmp/kassa-cash-*.png`. Смотреть
 * надо на строку под полем суммы и под кнопкой: по ней кассир понимает,
 * почему действие недоступно. Закрытая смена и нехватка денег в ящике поле
 * красным не красят — введено верно, мешает состояние кассы.
 *
 * Набор цифр сцене недоступен: поле принимает знаки только от настоящей
 * клавиатуры окна. Поэтому состояния, которые зависят от набранного,
 * проверены правилами в [MoneyCashRulesTest], а здесь сняты те, что
 * зависят от состояния кассы.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KassaMoneyLookTest {

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    private fun cashDocument(no: Long, type: String, tiyn: Long) =
        CoreScene.document("cash-$no", type = type, amount = tiyn, status = "SENT")
            .copy(docNo = no, createdAt = System.currentTimeMillis() - no * 600_000)

    /** Ящик с открытой сменой и прочитанными сутками. */
    private val drawer = CashUiState(
        kkm = CoreScene.kkm(),
        signedIn = true,
        shiftOpen = true,
        cashInDrawer = 125_000,
        recentRead = true,
        recentLoading = false
    )

    @Test
    fun `денежный ящик собирается во всех состояниях и они различимы`() {
        val frames = mapOf(
            "filled" to KassaScene.shot("cash-recent") {
                val recent = listOf(cashDocument(1, "CASH_IN", 500_000), cashDocument(2, "CASH_OUT", 150_000))
                CashContent(drawer.copy(recent = recent))
            },
            "empty" to KassaScene.shot("cash-recent-empty") { CashContent(drawer) },
            "shift-closed" to KassaScene.shot("cash-shift-closed") { CashContent(drawer.copy(shiftOpen = false)) },
            "unknown-balance" to KassaScene.shot("cash-balance-unknown") {
                CashContent(drawer.copy(cashInDrawer = null, recentRead = false))
            },
            "blocked" to KassaScene.shot("cash-kkm-blocked") {
                CashContent(drawer.copy(kkm = CoreScene.kkm(state = "BLOCKED")))
            }
        )

        frames.forEach { (name, frame) -> assertTrue(frame.isNotEmpty(), "пустой кадр: $name") }
        assertTrue(
            frames.values.map { it.toList() }.distinct().size == frames.size,
            "состояния денежного ящика неотличимы друг от друга"
        )
    }
}
