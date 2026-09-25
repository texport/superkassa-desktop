package kz.mybrain.superkassa

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Платформенный код — только то, что платформа делает иначе.
 *
 * Каждый файл платформенных наборов основного кода модулей — `jvmMain`,
 * `androidMain`, `iosMain` и любых других, кроме `commonMain`, — записан
 * в `platform-code.txt` с причиной. Новый файл без записи проверку не
 * проходит: его место — общий код, пока причина не названа. Запись без
 * файла не проходит тоже: список не копит того, чего уже нет.
 *
 * Точки входа `desktopApp` и `androidApp` — платформенные целиком и в счёт
 * не идут: у них нет общего кода.
 */
class PlatformCodeTest {

    @Test
    fun `каждый платформенный файл записан с причиной`() {
        val listed = listed()
        val found = platformFiles()

        assertEquals(emptyList(), found.filterNot { file -> listed.keys.any { covers(it, file) } }, UNLISTED)
        assertEquals(emptyList(), listed.keys.filterNot { entry -> found.any { covers(entry, it) } }, STALE)
        assertEquals(emptyList(), listed.filterValues { it.isBlank() }.keys.toList(), NO_REASON)
    }

    /** Запись покрывает файл: тот же путь или каталог, в котором он лежит. */
    private fun covers(entry: String, file: String) = file == entry || entry.endsWith("/") && file.startsWith(entry)

    /** Файлы платформенных наборов всех модулей — путями от корня сборки. */
    private fun platformFiles(): List<String> = ROOT.walkTopDown()
        .onEnter { it.name != "build" && !it.name.startsWith(".") }
        .filter { it.isDirectory && it.parentFile.name == "src" && it.name.endsWith("Main") && it.name != "commonMain" }
        .flatMap { set -> set.walkTopDown().filter { it.extension == "kt" } }
        .map { it.relativeTo(ROOT).invariantSeparatorsPath }
        .sorted()
        .toList()

    /** Записи списка: путь и причина. */
    private fun listed(): Map<String, String> = checkNotNull(javaClass.getResource("/$LIST")) { "no $LIST" }
        .readText()
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .associate { it.substringBefore(SEPARATOR).trim() to it.substringAfter(SEPARATOR, "").trim() }

    private companion object {
        /** Корень сборки: Gradle запускает проверки из каталога модуля. */
        val ROOT: File = File("..").canonicalFile

        const val LIST = "platform-code.txt"
        const val SEPARATOR = " — "
        const val UNLISTED = "платформенный файл без записи в $LIST: перенесите его в общий код или назовите причину"
        const val STALE = "запись в $LIST без файла: вычеркните её"
        const val NO_REASON = "запись в $LIST без причины"
    }
}
