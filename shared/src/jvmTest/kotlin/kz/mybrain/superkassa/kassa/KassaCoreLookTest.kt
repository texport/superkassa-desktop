package kz.mybrain.superkassa.kassa

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.KassaDesk
import kz.mybrain.superkassa.KassaExtremes
import kz.mybrain.superkassa.KassaProbe
import kz.mybrain.superkassa.KassaWindow
import kz.mybrain.superkassa.designsystem.theme.Look
import kz.mybrain.superkassa.designsystem.theme.TextScale
import kz.mybrain.superkassa.designsystem.theme.color.Appearance
import kz.mybrain.superkassa.label
import kz.mybrain.superkassa.presentation.kassa.cash.CashContent
import kz.mybrain.superkassa.presentation.kassa.cash.cashModel
import kz.mybrain.superkassa.presentation.kassa.refund.ReturnsContent
import kz.mybrain.superkassa.presentation.kassa.refund.returnsModel
import kz.mybrain.superkassa.presentation.kassa.sale.SaleContent
import kz.mybrain.superkassa.presentation.kassa.sale.SaleUiState
import kz.mybrain.superkassa.presentation.kassa.sale.SaleViewModel
import kz.mybrain.superkassa.presentation.kassa.sale.saleModel
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.presentation.shift.dashboard.DashboardContent
import kz.mybrain.superkassa.presentation.shift.dashboard.dashboardModel
import kz.mybrain.superkassa.presentation.words.kassa.action
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.wholeOnScreen
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Экраны кассы и смены, собранные на настоящем ядре, во всех окнах —
 * светлые с обычным шрифтом по-русски и тёмные с крупным по-казахски.
 *
 * Меряется, что главное действие экрана видно целиком, а на главном
 * экране — ещё и заголовок отклонённых БФД документов: на невысоком окне
 * с крупным шрифтом карточке отказов доставалась полоска, и текст резался
 * посреди строки. Обрезку сумм
 * и названий меряют проверки вида каждого экрана на предельных данных,
 * а здесь её ищут глазами. Кадры — `/tmp/qa-kassa-<экран>-<окно>-<вид>.png`: на них
 * смотрят глазами.
 */
class KassaCoreLookTest {
    private val desk = CoreDesk()

    @AfterTest
    fun close() = desk.close()

    /** Экран: имя кадра, главное действие и содержимое окна. */
    private class Screen(
        val name: String,
        val section: Section,
        val main: (Language) -> List<String>,
        val content: @Composable () -> Unit
    )

    /** Смена с продажей, внесением и отклонённым БФД чеком; чек на экране продажи — предельный. */
    private fun saleOnBusyShift(): SaleUiState {
        desk.seated(admin = true).run {
            sell("1250.50", "3")
            cashIn("5000.00")
            rejectedSale()
        }
        return saleModel(desk.services, desk.kassaPorts).apply {
            visit()
            add(KassaExtremes.LONG_NAME, "1234567.89", quantity = "12")
            add(KassaExtremes.KAZAKH_WORD, "450")
            form.taken("99999999")
        }.state.value
    }

    /** Позиция руками, как её набирает кассир: штуками по ОКЕИ. */
    private fun SaleViewModel.add(name: String, price: String, quantity: String = "1") {
        val draft = state.value.draft.copy(name = name, price = price, quantity = quantity, measureUnitCode = PIECE)
        entry.editDraft(draft)
        assertTrue(entry.addDraft(), "позиция «$name» не встала в чек")
    }

    private fun screens(): List<Screen> {
        val sale = saleOnBusyShift()
        val dashboard = dashboardModel(desk.services).state.value
        val returns = returnsModel(desk.services, desk.kassaPorts).apply {
            visit()
            choose(state.value.candidates.last())
        }.state.value
        val cash = cashModel(desk.services).apply { visit() }.state.value
        val closeAndRefused = { language: Language ->
            listOf(textsOf(language).common.dashboard.closeShift, refusedHead(language))
        }
        return listOf(
            Screen("dashboard", Section.Dashboard, closeAndRefused) { DashboardContent(dashboard) },
            Screen("sale", Section.Sale, { listOf(sale.form.operation.action(textsOf(it).common.receipt)) }) {
                SaleContent(sale)
            },
            Screen("returns", Section.Returns, { listOf(returns.kind.action(textsOf(it).common.returns)) }) {
                // Чек выбран: на узком окне он открыт поверх списка шагом истории.
                ReturnsContent(returns, stepped = true)
            },
            Screen("cash", Section.Cash, { listOf(textsOf(it).common.cash.deposit) }) { CashContent(cash) }
        )
    }

    private fun check(probe: KassaProbe, screen: Screen, look: Look, language: Language, tag: String): List<String> {
        val window = KassaDesk(desk.app())
        probe.show(look, language) { KassaWindow(window, screen.section) { screen.content() } }
        File("/tmp/qa-kassa-${screen.name}-$tag.png").writeBytes(probe.frame(KassaProbe.SETTLE))
        val failures = mutableListOf<String>()
        screen.main(language).forEach { text ->
            val node = probe.parts().firstOrNull { it.label() == text }
            val seen = node?.wholeOnScreen(probe.width, probe.height) == true && !probe.squeezed(node)
            if (!seen) failures += "${screen.name} $tag: «$text» не видно или сжато"
        }
        return failures
    }

    @Test
    fun `главное действие видно во всех окнах, в светлом и тёмном виде`() {
        val screens = screens()
        val failures = KassaExtremes.WINDOWS.flatMap { (width, height) ->
            VIEWS.flatMap { view ->
                KassaProbe(width, height, view.appearance).use { probe ->
                    screens.flatMap { check(probe, it, view.look, view.language, "${width}x$height-${view.tag}") }
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    /** Заголовок карточки отклонённых БФД документов: в сценарии отказ один. */
    private fun refusedHead(language: Language): String = "${textsOf(language).common.dashboard.refused}: 1"

    /** Вид окна: светлый с обычным шрифтом по-русски, тёмный с крупным по-казахски. */
    private class View(val appearance: Appearance, val look: Look, val language: Language, val tag: String)

    private companion object {
        val VIEWS = listOf(
            View(Appearance.Light, Look(textScale = TextScale.Normal), Language.Ru, "light"),
            View(Appearance.Dark, Look(textScale = TextScale.Larger), Language.Kk, "dark-larger-kk")
        )

        /** Штука по ОКЕИ. */
        const val PIECE = "796"
    }
}
