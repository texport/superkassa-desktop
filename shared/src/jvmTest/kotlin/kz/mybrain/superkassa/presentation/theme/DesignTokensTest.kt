package kz.mybrain.superkassa.presentation.theme

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Размеры оформления — только из общей шкалы.
 *
 * Числа в точках (`16.dp`) и в пунктах шрифта (`15.sp`) пишутся в одном
 * месте: шаг шкалы — в `theme/size/Spacing.kt`, шрифтовая шкала —
 * в `theme/type`. Экраны и наборы размеров берут готовое: отступ,
 * записанный числом на месте, расходился с соседним экраном на пару
 * точек, и найти причину в десятках файлов было нельзя.
 */
class DesignTokensTest {

    @Test
    fun `вне оформления нет чисел в точках и пунктах`() {
        val found = sources(APP_SOURCES).filterNot { it.first.contains("/presentation/theme/") }.flatMap(::literals)
        assertTrue(found.isEmpty(), "числа размеров вне theme:\n" + found.joinToString("\n"))
    }

    @Test
    fun `наборы размеров ссылаются на шаг шкалы, а не заводят свои числа`() {
        val found = sources(listOf(SIZES)).filterNot { it.first.endsWith("/Spacing.kt") }.flatMap(::literals)
        assertTrue(found.isEmpty(), "свои числа в наборах размеров:\n" + found.joinToString("\n"))
    }

    /** Файлы исходников: путь и строки. */
    private fun sources(roots: List<String>): List<Pair<String, List<String>>> = roots
        .map(::File)
        .filter { it.isDirectory }
        .flatMap { root -> root.walkTopDown().filter { it.extension == "kt" }.toList() }
        .map { it.invariantSeparatorsPath to it.readLines() }

    /** Строки с числом в точках или пунктах; комментарии не в счёт. */
    private fun literals(source: Pair<String, List<String>>): List<String> =
        source.second.withIndex()
            .filter { (_, line) -> !line.trimStart().startsWith("*") && !line.trimStart().startsWith("//") }
            .filter { (_, line) -> LITERAL.containsMatchIn(line.substringBefore("//")) }
            .map { (at, line) -> "${source.first}:${at + 1}: ${line.trim()}" }

    private companion object {
        /** Число прямо в точках или пунктах: `16.dp`, `0.5.dp`, `15.sp`, `2f.dp`. */
        val LITERAL = Regex("""(?<![\w.])\d+(\.\d+)?f?\.(dp|sp)\b""")

        const val KOTLIN = "kotlin/kz/mybrain/superkassa"
        const val SIZES = "src/commonMain/$KOTLIN/presentation/theme/size"
        val APP_SOURCES = listOf(
            "src/commonMain/$KOTLIN",
            "src/jvmMain/$KOTLIN",
            "src/androidMain/$KOTLIN",
            "../desktopApp/src/main/$KOTLIN",
            "../androidApp/src/main/$KOTLIN"
        )
    }
}
