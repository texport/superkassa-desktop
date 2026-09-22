package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import kz.mybrain.superkassa.desktop.ui.KkmTopBar
import kz.mybrain.superkassa.desktop.ui.Section
import kz.mybrain.superkassa.desktop.ui.SectionRail
import kz.mybrain.superkassa.desktop.ui.components.ScreenTitle
import kz.mybrain.superkassa.desktop.ui.components.SectionTitle
import kz.mybrain.superkassa.desktop.ui.sale.Basket
import kz.mybrain.superkassa.desktop.ui.sale.Position
import kz.mybrain.superkassa.desktop.ui.sale.SaleForm
import kz.mybrain.superkassa.desktop.ui.sale.SaleHeader
import kz.mybrain.superkassa.desktop.ui.sale.SaleScreen
import java.io.File
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Заголовок продажи в узком окне.
 *
 * Окно кассы бывает ростом в 700 точек и шириной в 1000: за вычетом
 * рельса разделов и кассовой колонки листу чека остаётся около четверти
 * ширины. Заголовок «Чек» набирался своим `Text` без ограничения строк,
 * и в этой четверти он рассыпался столбиком по одной букве — ровно тем
 * же способом, каким когда-то рассыпался счётчик строк в журнале.
 *
 * Проверяется не картинка, а высота: строка заголовка обязана остаться
 * одной строкой, а не превратиться в столбец. Кадр рядом — чтобы
 * смотреть глазами.
 */
class SaleHeadingLayoutTest {

    /** Высота того, что нарисовалось в колонке заданной ширины. */
    private fun heightAt(width: Int, content: @Composable () -> Unit): Int {
        var height = 0
        RenderProbe(width = width, height = HEIGHT) {
            Box(modifier = Modifier.onGloballyPositioned { height = it.size.height }) { content() }
        }.use { it.frame() }
        return height
    }

    @Composable
    private fun Header() {
        val basket = Basket().apply { add(POSITION) }
        SaleHeader(SaleForm(), basket)
    }

    /**
     * Само название — одной строкой при любой ширине.
     *
     * Проверяются общие заголовки, а не копия в экране продажи: рассыпался
     * именно свой `Text`, набранный мимо них.
     */
    @Test
    fun `общие заголовки остаются строкой даже в колонке шириной в ладонь`() {
        val screen = heightAt(TIGHT) { ScreenTitle(LONG_TITLE) }
        val section = heightAt(TIGHT) { SectionTitle(LONG_TITLE) }
        println("заголовки в колонке $TIGHT px: экран — $screen px, раздел — $section px")
        assertTrue(screen in 1..ONE_ROW, "заголовок экрана занял $screen px — это столбец, а не строка")
        assertTrue(section in 1..ONE_ROW, "заголовок раздела занял $section px — это столбец, а не строка")
    }

    @Test
    fun `заголовок чека переносит ряд, а не рассыпается по буквам`() {
        val roomy = heightAt(WIDE) { Header() }
        val narrow = heightAt(RECEIPT_COLUMN) { Header() }
        val tight = heightAt(TIGHT) { Header() }
        println("заголовок чека: $WIDE px — $roomy px, $RECEIPT_COLUMN px — $narrow px, $TIGHT px — $tight px")
        assertTrue(roomy in 1..ONE_ROW, "заголовок в широком окне занял $roomy px")
        assertTrue(narrow <= ONE_ROW * ROWS, "заголовок в узкой колонке занял $narrow px — больше $ROWS рядов")
        assertTrue(tight <= ONE_ROW * ROWS, "заголовок в колонке $TIGHT px занял $tight px — больше $ROWS рядов")
    }

    /**
     * Шапка окна в той же ширине.
     *
     * Над экраном продажи стоит шапка с кассой и кассиром, а справа в ней
     * пять действий: в узком окне названию кассы остаётся немного. Оно
     * обязано сократиться, а не встать столбиком, — иначе шапка отнимет
     * у чека половину высоты.
     */
    @Test
    fun `название кассы в шапке узкого окна остаётся строкой`() {
        val session = KassaScene.session("sale-headings-bar", shift = KassaScene.openShift())
        val chosen = heightAt(LOW_WINDOW_WIDTH) { KkmTopBar(session, onSignOut = {}, onRefresh = {}) }
        // Касса может быть и не выбрана: тогда в заголовке стоит «Касса
        // не выбрана» — строка вдвое длиннее номера, и места ей ещё меньше.
        val none = KassaScene.session("sale-headings-bar-none", kkm = null)
        val empty = heightAt(LOW_WINDOW_WIDTH) { KkmTopBar(none, onSignOut = {}, onRefresh = {}) }
        println("шапка окна $LOW_WINDOW_WIDTH px: с кассой — $chosen px, без кассы — $empty px")
        assertTrue(chosen in 1..(ONE_ROW * ROWS), "шапка заняла $chosen px — название кассы рассыпалось")
        assertTrue(empty in 1..(ONE_ROW * ROWS), "шапка без кассы заняла $empty px")
    }

    /**
     * Окно 1000×700 целиком: рельс разделов и экран продажи рядом.
     *
     * Кадр собран в том же составе, в каком владелец видит окно: ширину
     * листу чека оставляет рельс, а не одна лишь кассовая колонка.
     */
    @Test
    fun `экран продажи в низком узком окне собирается целиком`() {
        val session = KassaScene.session("sale-headings", shift = KassaScene.openShift())
        val frame = RenderProbe(width = LOW_WINDOW_WIDTH, height = LOW_WINDOW_HEIGHT) {
            Row(modifier = Modifier.fillMaxSize()) {
                SectionRail(
                    sections = Section.entries,
                    current = Section.Sale,
                    collapsed = false,
                    onToggle = {},
                    footer = { Text(VERSION) },
                    onPick = {}
                )
                SaleScreen(session)
            }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.frame()
        }
        File(SHOT).writeBytes(frame)
        assertTrue(frame.isNotEmpty(), "кадр узкого окна пуст")
    }

    private companion object {
        const val HEIGHT = 400
        const val WIDE = 1180

        /** Столько остаётся листу чека при окне 1000×700: рельс и кассовая колонка своё уже взяли. */
        const val RECEIPT_COLUMN = 260
        const val TIGHT = 120
        const val ONE_ROW = 72
        const val ROWS = 3
        const val LONG_TITLE = "Оплата и итог по чеку"
        const val LOW_WINDOW_WIDTH = 1000
        const val LOW_WINDOW_HEIGHT = 700
        const val SETTLE = 40
        const val VERSION = "1.0.0"
        const val SHOT = "/tmp/sale-headings-window-1000x700.png"

        val POSITION = Position(
            name = "Баранина на косточке, охлаждённая",
            price = BigDecimal("3450.00"),
            quantity = BigDecimal("1.450"),
            vatGroup = "VAT_16",
            measureUnitCode = "166"
        )
    }
}
