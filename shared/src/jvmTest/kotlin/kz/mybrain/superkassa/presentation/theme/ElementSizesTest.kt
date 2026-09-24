package kz.mybrain.superkassa.presentation.theme

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Одна сетка на экран: ширину и высоту элемента задаёт раскладка.
 *
 * Поле, плашка или карточка берут ширину из колонки (`fillMaxWidth`) или
 * долю ряда (`weight`), а не свой токен: поэлементные ширины разводили
 * края полей одного экрана на десятки точек. Свой размер — только у того,
 * у кого он свой по смыслу ([ALLOWED]): значок, рельс, колонка таблицы,
 * диалог Material 3, колонка входа, цель нажатия, зазор шкалы и размер,
 * измеренный самой раскладкой.
 *
 * Места, с которыми код пришёл к проверке, перечислены в `size-debt.txt`
 * как `файл -> токен`: долг только сокращается. Новое место падает сразу,
 * убранное требует вычеркнуть строку.
 */
class ElementSizesTest {

    @Test
    fun `новых поэлементных размеров нет`() {
        val fresh = violations() - debt()
        assertTrue(fresh.isEmpty(), "поэлементные размеры вне белого списка:\n" + fresh.sorted().joinToString("\n"))
    }

    @Test
    fun `долг перечисляет только то, что ещё есть`() {
        assertEquals(emptySet(), debt() - violations(), "размеры убраны — вычеркните их из $DEBT")
    }

    private fun violations(): Set<String> = ROOTS.map(::File).filter { it.isDirectory }.flatMap { root ->
        root.walkTopDown().filter { it.extension == "kt" && "/presentation/theme/" !in it.invariantSeparatorsPath }
            .flatMap { file -> sized(file.relativeTo(root).invariantSeparatorsPath, file.readLines()) }
    }.toSet()

    /** Токены размеров в поэлементных вызовах файла: `путь -> Токен.имя`. */
    private fun sized(path: String, lines: List<String>): List<String> = lines
        .filterNot { it.trimStart().startsWith("*") || it.trimStart().startsWith("//") }
        .flatMap { line -> CALL.findAll(line).map { line.substring(it.range.last + 1) } }
        .flatMap { args -> TOKEN.findAll(args).map { it.value } }
        .filterNot { token -> ALLOWED.any { it.matches(token) } }
        .map { "$path -> $it" }

    private fun debt(): Set<String> = javaClass.getResource("/$DEBT")!!.readText().lineSequence()
        .map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toSet()

    private companion object {
        const val DEBT = "size-debt.txt"
        const val SIZE_CALLS = "width|widthIn|requiredWidth|defaultMinSize|sizeIn|height|heightIn|size|" +
            "requiredHeight|requiredSize|fieldWidth"
        const val KOTLIN = "kotlin/kz/mybrain/superkassa"
        val ROOTS = listOf("src/commonMain/$KOTLIN", "src/jvmMain/$KOTLIN", "src/androidMain/$KOTLIN")

        /** Поэлементный размер: ширина, высота, размер и их пределы. */
        val CALL = Regex("""\.($SIZE_CALLS)\(""")

        /** Токен размера из наборов оформления. */
        val TOKEN = Regex("""\b(Sizes|KassaLayout|HistoryLayout|AnalyticsLayout|ContentWidths|CardGrid|Tape)\.\w+""")

        /** Свой размер по смыслу: значки, рельс, диалог, колонка входа, высота поля и полоски ожидания. */
        val ALLOWED = listOf(
            Regex("""Sizes\.\w*(Icon|icon|Circle|Swatch|Dot)\w*"""),
            Regex("""Sizes\.(rail|formDialog|loginColumn|fieldHeight|busyLine|chipHeight)""")
        )
    }
}
