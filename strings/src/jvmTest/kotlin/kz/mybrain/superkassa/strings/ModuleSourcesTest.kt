package kz.mybrain.superkassa.strings

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Устройство модуля по его исходникам: реализация закрыта, открытое описано.
 *
 * Снаружи модуля виден только `api`. Компилятор не даст приложению вызвать
 * `internal`, но не заметит объявление `impl`, которое забыли закрыть: оно
 * молча станет частью открытого вида модуля. Проверка читает исходники,
 * поэтому живёт в наборе проверок JVM — чтение файлов есть только там.
 */
class ModuleSourcesTest {

    @Test
    fun `в impl нет открытых объявлений`() {
        sources("impl").forEach { file ->
            file.readLines().forEachIndexed { at, line ->
                assertTrue(!OPEN.matches(line), "${file.name}:${at + 1} открыто наружу: $line")
            }
        }
    }

    /**
     * Каждое открытое объявление `api` — класс, функция, точка входа —
     * с описанием: модуль читают снаружи, не открывая `impl`.
     */
    @Test
    fun `открытые объявления api описаны`() {
        sources("api").forEach { file ->
            val lines = file.readLines()
            lines.forEachIndexed { at, line ->
                if (DECLARATIONS.any { it.matches(line) }) {
                    val above = lines.subList(0, at).lastOrNull { it.isNotBlank() && !it.trimStart().startsWith("@") }
                    assertTrue(above?.trim()?.endsWith("*/") == true, "${file.name}:${at + 1} без описания: $line")
                }
            }
        }
    }

    private fun sources(part: String): List<File> {
        val root = File("src/commonMain/kotlin/kz/mybrain/superkassa/strings/$part")
        assertTrue(root.isDirectory, "нет исходников ${root.absolutePath}")
        return root.walkTopDown().filter { it.extension == "kt" }.toList()
    }

    private companion object {
        /** Объявление верхнего уровня без `internal` и `private`. */
        val OPEN = Regex("""^(?!internal |private )([a-z]+ )*(class|object|interface|fun|val|var|typealias)\b.*""")

        /** Открытое объявление `api`: верхнего уровня или функция формы. */
        val DECLARATIONS = listOf(
            Regex("""^(data |enum )?(class|object|interface|fun|typealias|val|const val).*"""),
            Regex("""^ {4}fun.*""")
        )
    }
}
