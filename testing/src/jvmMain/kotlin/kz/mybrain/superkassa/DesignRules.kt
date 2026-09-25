package kz.mybrain.superkassa

import java.io.File

/**
 * Размеры оформления по исходникам: числа и свои размеры — только в токенах.
 *
 * Числа в точках (`16.dp`) и пунктах (`15.sp`) пишутся в одном месте —
 * в токенах дизайн-системы [THEME]: отступ, записанный числом на месте,
 * расходился с соседним экраном на пару точек. Ширину и высоту элемента
 * задаёт раскладка — колонка (`fillMaxWidth`) или доля ряда (`weight`), —
 * а не свой токен: поэлементные ширины разводили края полей одного экрана.
 *
 * Правило одно на всё приложение, а проверяет его каждый модуль у себя:
 * `designsystem` — токены и общие компоненты, `shared` — экраны и точки
 * входа. Gradle запускает проверки из каталога модуля, и пути здесь — от него.
 */
object DesignRules {
    /** Каталог токенов от корня пакетов приложения: только здесь свои числа и размеры. */
    const val THEME = "designsystem/theme/"

    private const val PREFIX = "kotlin/kz/mybrain/superkassa"
    private val SETS = listOf("commonMain", "jvmMain", "androidMain", "iosMain", "main")

    /**
     * Основной код модуля построчно: путь от корня пакетов приложения
     * и строки файла.
     *
     * @param module каталог модуля от каталога проверяемого; путь чужого
     *   модуля начинается с его каталога: `../desktopApp/Assembly.kt`.
     */
    fun code(module: String = "."): Map<String, List<String>> = SETS
        .map { File("$module/src/$it/$PREFIX") }
        .filter { it.isDirectory }
        .flatMap { root -> root.walkTopDown().filter { it.extension == "kt" }.map { root to it } }
        .associate { (root, file) -> named(module, file.relativeTo(root).invariantSeparatorsPath) to file.readLines() }

    private fun named(module: String, path: String): String = if (module == ".") path else "$module/$path"

    /** Строки с числом в точках или пунктах: `путь:строка: текст`; комментарии не в счёт. */
    fun literals(path: String, lines: List<String>): List<String> = lines.withIndex()
        .filterNot { (_, line) -> comment(line) }
        .filter { (_, line) -> LITERAL.containsMatchIn(line.substringBefore("//")) }
        .map { (at, line) -> "$path:${at + 1}: ${line.trim()}" }

    /**
     * Поэлементные размеры вне белого списка: `путь -> Токен.имя`.
     *
     * Свой размер — только у того, у кого он свой по смыслу ([ALLOWED]):
     * значок, рельс, диалог Material 3, колонка входа, высота поля
     * и полоски ожидания.
     */
    fun elementSizes(path: String, lines: List<String>): List<String> = lines
        .filterNot(::comment)
        .flatMap { line -> CALL.findAll(line).map { line.substring(it.range.last + 1) } }
        .flatMap { args -> TOKEN.findAll(args).map { it.value } }
        .filterNot { token -> ALLOWED.any { it.matches(token) } }
        .map { "$path -> $it" }

    private fun comment(line: String): Boolean = line.trimStart().let { it.startsWith("*") || it.startsWith("//") }

    /** Число прямо в точках или пунктах: `16.dp`, `0.5.dp`, `15.sp`, `2f.dp`. */
    private val LITERAL = Regex("""(?<![\w.])\d+(\.\d+)?f?\.(dp|sp)\b""")

    private const val SIZE_CALLS = "width|widthIn|requiredWidth|defaultMinSize|sizeIn|height|heightIn|size|" +
        "requiredHeight|requiredSize|fieldWidth"

    /** Поэлементный размер: ширина, высота, размер и их пределы. */
    private val CALL = Regex("""\.($SIZE_CALLS)\(""")

    /** Токен размера из наборов оформления. */
    private val TOKEN =
        Regex("""\b(Sizes|KassaLayout|HistoryLayout|AnalyticsLayout|ContentWidths|CardGrid|Tape)\.\w+""")

    /**
     * Свой размер по смыслу: значки, рельс, диалог, колонка входа, высота поля
     * и полоски ожидания, QR подписи — его читает телефон, и размер задан этим.
     */
    private val ALLOWED = listOf(
        Regex("""Sizes\.\w*(Icon|icon|Circle|Swatch|Dot)\w*"""),
        Regex("""Sizes\.(rail|formDialog|loginColumn|fieldHeight|busyLine|chipHeight|signQr)""")
    )
}
