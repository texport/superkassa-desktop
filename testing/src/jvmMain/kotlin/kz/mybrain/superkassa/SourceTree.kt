package kz.mybrain.superkassa

import java.io.File

/**
 * Исходник модуля, как его видят проверки устройства: путь, пакет и импорты
 * кода приложения.
 *
 * @property path путь от корня пакетов приложения: `domain/kassa/port/Kassa.kt`.
 * @property pkg пакет файла; `null` — файл без пакета.
 * @property imports импорты из `kz.mybrain.superkassa`, без псевдонимов.
 */
class Source(val path: String, val pkg: String?, val imports: List<String>)

/**
 * Исходники модуля, из которого запущена проверка, и списки долга.
 *
 * Проверки устройства — слои, области, открытое наружу — читают код
 * модуля как текст: так они видят то, чего не видит компилятор. Gradle
 * запускает проверки из каталога модуля, и пути здесь — от него.
 */
object SourceTree {
    /** Корневой пакет приложения. */
    const val ROOT = "kz.mybrain.superkassa"

    private const val PREFIX = "kotlin/kz/mybrain/superkassa"
    private val MAIN_SETS = listOf("commonMain", "jvmMain", "androidMain", "iosMain")

    /** Основные наборы модуля — общий и платформенные; отсутствующих нет. */
    fun main(): List<Source> = MAIN_SETS
        .map { File("src/$it/$PREFIX") }
        .filter { it.isDirectory }
        .flatMap { root -> root.walkTopDown().filter { it.extension == "kt" }.map { root to it } }
        .map { (root, file) -> of(file.relativeTo(root).invariantSeparatorsPath, file.readLines()) }

    /** Наборы основного кода, которые у модуля есть: `commonMain`, `jvmMain`, `androidMain`, `iosMain`. */
    fun mainSets(): List<String> = MAIN_SETS.filter { File("src/$it").isDirectory }

    /**
     * Строки долга из ресурса проверок [name]: без пустых строк и пояснений.
     *
     * Долг — нарушения, с которыми код пришёл к проверке; строка
     * вычёркивается, когда нарушение исправлено.
     */
    fun debt(name: String): Set<String> = checkNotNull(javaClass.getResource("/$name")) { "no $name" }
        .readText()
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .toSet()

    /** То же, что [debt], но модуль без такого списка долга не должен ничего. */
    fun debtOrEmpty(name: String): Set<String> =
        if (javaClass.getResource("/$name") == null) emptySet() else debt(name)

    /**
     * Открытые объявления верхнего уровня без описания: `файл:строка объявление`.
     *
     * Открытое — без `internal` и `private`; описание — KDoc над ним,
     * аннотации между ними не в счёт. Модуль читают снаружи, не открывая
     * его исходников.
     */
    fun undocumented(): List<String> = MAIN_SETS
        .map { File("src/$it/kotlin") }
        .filter { it.isDirectory }
        .flatMap { root -> root.walkTopDown().filter { it.extension == "kt" } }
        .flatMap { file ->
            val lines = file.readLines()
            lines.withIndex()
                .filter { (_, line) -> DECLARATION.matches(line) }
                .filterNot { (at, _) -> documented(lines, at) }
                .map { (at, line) -> "${file.name}:${at + 1} $line" }
        }

    /** Слой пакета: `domain`, `data`, `presentation`; `null` — не код приложения. */
    fun layerOf(name: String?): String? =
        name?.takeIf { it.startsWith("$ROOT.") }?.removePrefix("$ROOT.")?.substringBefore('.')

    private fun documented(lines: List<String>, at: Int): Boolean =
        lines.subList(0, at).lastOrNull { it.isNotBlank() && !it.trimStart().startsWith("@") }
            ?.trim()?.endsWith("*/") == true

    /** Открытое объявление верхнего уровня: без `internal` и `private`. */
    private val DECLARATION = Regex(
        """^((data|enum|sealed|abstract|open|value|fun|const|suspend|operator|inline) )*""" +
            """(class|object|interface|fun|typealias|val)\b.*"""
    )

    private fun of(path: String, lines: List<String>) = Source(
        path = path,
        pkg = lines.firstOrNull { it.startsWith("package ") }?.removePrefix("package ")?.trim(),
        imports = lines.filter { it.startsWith("import $ROOT.") }
            .map { it.removePrefix("import ").substringBefore(" as ").trim() }
    )
}
