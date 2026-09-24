package kz.mybrain.superkassa

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Границы областей: область не видит внутренностей другой области.
 *
 * Область — второй уровень пакета в `presentation` и `domain`:
 * `presentation/kassa` и `domain/kassa` со всеми подпакетами — одна
 * область `kassa`.
 * Из чужой области брать нельзя ничего; общее лежит на общих полках,
 * которые видны всем:
 *
 * - `presentation/common`, `presentation/theme`, `presentation/strings`
 *   и `presentation/shell` (каркас окна: он собирает области и видит их все);
 * - общие домены [SHARED_DOMAINS]: касса и её ответ, вход, журнал
 *   приложения, правила кассы и фискального документа, смена, версия
 *   и рабочее место — то, о чём спрашивает каждая область.
 *
 * Нарушения, с которыми код пришёл к этой проверке, перечислены
 * в `area-debt.txt`: долг снимается при переводе своей области. Новое
 * нарушение падает сразу, а исправленное требует вычеркнуть строку.
 *
 * Тем же порядком проверяется шаблон модели экрана: файл `*ViewModel.kt`
 * не видит ни контейнера окна, ни держателя входа, ни портов — только
 * сценарии своей области ([MODEL_FORBIDDEN]). Модели, ещё не переведённые
 * на шаблон, перечислены в `model-debt.txt`.
 */
class AreaBoundariesTest {

    @Test
    fun `no area imports the inside of another area`() {
        val fresh = violations() - debt(AREA_DEBT)
        assertTrue(fresh.isEmpty(), "new area violations:\n" + fresh.sorted().joinToString("\n"))
    }

    @Test
    fun `debt lists only violations that still exist`() {
        val paid = debt(AREA_DEBT) - violations()
        assertEquals(emptySet(), paid, "violations fixed, remove them from $AREA_DEBT")
    }

    @Test
    fun `view models see only use cases`() {
        val fresh = modelViolations() - debt(MODEL_DEBT)
        assertTrue(fresh.isEmpty(), "view models reaching past use cases:\n" + fresh.sorted().joinToString("\n"))
    }

    @Test
    fun `model debt lists only models still off the pattern`() {
        val paid = debt(MODEL_DEBT) - modelViolations()
        assertEquals(emptySet(), paid, "models moved to the pattern, remove them from $MODEL_DEBT")
    }

    /** Модель экрана, которая берёт больше своих сценариев: `путь -> импорт`. */
    private fun modelViolations(): Set<String> = sources()
        .filter { it.path.startsWith("presentation/") && it.path.endsWith("ViewModel.kt") }
        .flatMap { source ->
            source.imports.filter { import -> MODEL_FORBIDDEN.any { it.matches(import) } }
                .map { "${source.path} -> ${it.removePrefix("$ROOT.")}" }
        }.toSet()

    private fun violations(): Set<String> {
        val sources = sources()
        val packages = sources.mapNotNull { it.pkg }.toSet()
        return sources.flatMap { source ->
            val own = areaOf(source.pkg) ?: return@flatMap emptyList()
            source.imports
                .mapNotNull { areaOf(packageOf(it, packages)) }
                .filter { it.name != own.name }
                .map { "${source.path} -> ${it.layer}.${it.name}" }
        }.toSet()
    }

    private fun sources(): List<Source> = SOURCE_SETS
        .map { File(it) }
        .filter { it.isDirectory }
        .flatMap { root -> root.walkTopDown().filter { it.extension == "kt" }.map { root to it } }
        .map { (root, file) -> Source.of(file.relativeTo(root).invariantSeparatorsPath, file.readLines()) }

    /** Область пакета; `null` — общая полка или не область вовсе. */
    private fun areaOf(name: String?): Area? {
        val parts = name?.takeIf { it.startsWith("$ROOT.") }?.removePrefix("$ROOT.")?.split('.')
        if (parts == null || parts.size < 2) return null
        val area = Area(parts[0], parts[1])
        val shared = when (area.layer) {
            "presentation" -> area.name in SHARED_PRESENTATION
            "domain" -> area.name in SHARED_DOMAINS
            else -> true
        }
        return area.takeUnless { shared }
    }

    /** Пакет импорта: самый длинный известный пакет, с которого он начинается. */
    private fun packageOf(import: String, packages: Set<String>): String =
        packages.filter { import.startsWith("$it.") }.maxByOrNull { it.length } ?: import

    private fun debt(name: String): Set<String> = javaClass.getResource("/$name")!!.readText()
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .toSet()

    private data class Area(val layer: String, val name: String)

    private class Source(val path: String, val pkg: String?, val imports: List<String>) {
        companion object {
            fun of(path: String, lines: List<String>) = Source(
                path = path,
                pkg = lines.firstOrNull { it.startsWith("package ") }?.removePrefix("package ")?.trim(),
                imports = lines.filter { it.startsWith("import $ROOT.") }
                    .map { it.removePrefix("import ").substringBefore(" as ").trim() }
            )
        }
    }

    private companion object {
        const val ROOT = "kz.mybrain.superkassa"
        const val PREFIX = "kotlin/kz/mybrain/superkassa"
        val SOURCE_SETS = listOf("src/commonMain/$PREFIX", "src/jvmMain/$PREFIX", "src/androidMain/$PREFIX")
        val SHARED_PRESENTATION = setOf("common", "theme", "strings", "shell")
        val SHARED_DOMAINS = setOf("kassa", "signin", "log", "kkm", "document", "shift", "version", "workplace")
        const val AREA_DEBT = "area-debt.txt"
        const val MODEL_DEBT = "model-debt.txt"

        /** Что модели экрана брать нельзя: весь контейнер окна, держатель входа, порты. */
        val MODEL_FORBIDDEN = listOf(
            Regex("""\Q$ROOT.presentation.shell.AppContainer\E"""),
            Regex("""\Q$ROOT.domain.signin.model.SignIn\E"""),
            Regex("""\Q$ROOT.domain.\E\w+\.port\..+""")
        )
    }
}
