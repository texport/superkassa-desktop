package kz.mybrain.superkassa.presentation.analytics.exchange

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.text.TextLayoutResult
import kz.mybrain.superkassa.Look
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.designsystem.theme.TextScale
import kz.mybrain.superkassa.domain.analytics.model.ExchangeAddress
import kz.mybrain.superkassa.presentation.analytics.AnalyticsLook
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kz.mybrain.superkassa.designsystem.theme.Look as Scale

/**
 * Адрес и время обмена читаются целиком в любом окне и на любой ступени шрифта.
 *
 * IPv6 — 39 знаков, а столбец адреса был постоянной ширины под IPv4:
 * в окне 960 и 1180 адрес обрывался многоточием посреди группы цифр,
 * и сверить его с журналом провайдера было нельзя; на крупной ступени
 * так же обрывалось время — «01.09.2026 1…».
 */
class AnalyticsExchangeWidthTest {

    private val rows = (0 until ROWS).map { at ->
        ExchangeAddress(
            cashRegisterId = "c$at",
            kkmId = FIRST_KKM + at,
            internalName = "Касса № $at — кіреберістегі үлкен",
            retailPlaceName = "Сауда орталығы «Бәйтерек-Нұр» № $at",
            address = "2a02:2168:a0f:%04x:9c5d:ffff:ffff:ffff".format(at),
            firstSeen = "2026-09-01T06:47:00Z",
            lastSeen = "2026-09-22T20:29:00Z"
        )
    }

    @Test
    fun `адрес и время обмена не обрываются ни в малом окне, ни на крупной ступени`() {
        listOf(
            Triple(NARROW, LOW, TextScale.Normal),
            Triple(DEFAULT_WIDTH, DEFAULT_HEIGHT, TextScale.Normal),
            Triple(NARROW, LOW, TextScale.Larger)
        ).forEach { (width, height, scale) ->
            val look = Scale(textScale = scale)
            RenderProbe(width, height, look = look) {
                AnalyticsExchangeList(rows, AnalyticsLook.texts, Modifier.fillMaxSize())
            }.use { probe ->
                repeat(SETTLE) { probe.frame() }
                Look.shot("qa-analytics-exchange-ipv6-${width}x$height-${scale.code}", probe.frame())
                val cut = probe.nodes { nodes -> nodes.filter { it.value() && cut(it) }.map { it.text() } }
                val shown = probe.nodes { nodes -> nodes.count { it.text().startsWith(PREFIX) } }
                assertTrue(shown > 0, "$width×$height ${scale.code}: адресов на экране нет")
                assertEquals(emptyList(), cut, "$width×$height ${scale.code}: адрес оборван")
            }
        }
    }

    /** Адрес или время обмена: их обрывать нельзя. */
    private fun SemanticsNode.value(): Boolean = text().startsWith(PREFIX) || MOMENT.matches(text())

    private fun SemanticsNode.text(): String =
        config.getOrNull(SemanticsProperties.Text)?.joinToString("") { it.text }.orEmpty()

    /** Обрезан ли текст узла многоточием или краем — по его собственной раскладке. */
    private fun cut(node: SemanticsNode): Boolean {
        val layouts = mutableListOf<TextLayoutResult>()
        node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(layouts)
        return layouts.any { layout ->
            // Высоту не спрашиваем: строка шрифта бывает на точку выше ячейки и без обрезки.
            layout.multiParagraph.didExceedMaxLines || (0 until layout.lineCount).any { line ->
                layout.isLineEllipsized(line) || layout.getLineRight(line) > layout.size.width + 1
            }
        }
    }

    private companion object {
        const val PREFIX = "2a02:"
        val MOMENT = Regex("\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}")
        const val ROWS = 40
        const val FIRST_KKM = 5_000_000
        const val NARROW = 960
        const val LOW = 640
        const val DEFAULT_WIDTH = 1180
        const val DEFAULT_HEIGHT = 820
        const val SETTLE = 12
    }
}
