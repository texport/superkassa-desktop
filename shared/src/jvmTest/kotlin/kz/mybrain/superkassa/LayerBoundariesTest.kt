package kz.mybrain.superkassa

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Границы слоёв: кто кого не видит.
 *
 * - `domain` не знает ни `data`, ни `presentation`;
 * - `presentation` не знает `data`;
 * - `data` не знает `presentation`.
 *
 * Связывает слои только точка сборки — `Main.kt` настольной кассы
 * и `MainActivity` Android, — и её эта проверка не касается.
 *
 * Нарушения, с которыми код пришёл в три слоя, перечислены в
 * `layer-debt.txt`: это долг, и он снимается при переводе своей области.
 * Новое нарушение падает сразу, а исправленное требует вычеркнуть строку
 * из списка — иначе список перестал бы говорить правду.
 */
class LayerBoundariesTest {

    @Test
    fun `no layer imports a layer it must not know`() {
        val fresh = violations() - debt()
        assertTrue(fresh.isEmpty(), "new layer violations:\n" + fresh.sorted().joinToString("\n"))
    }

    @Test
    fun `debt lists only violations that still exist`() {
        val paid = debt() - violations()
        assertEquals(emptySet(), paid, "violations fixed, remove them from layer-debt.txt")
    }

    private fun violations(): Set<String> {
        val sources = sources()
        val packages = sources.mapNotNull { it.pkg }.toSet()
        return sources.flatMap { source ->
            val forbidden = FORBIDDEN[layerOf(source.pkg)].orEmpty()
            source.imports
                .map { packageOf(it, packages) }
                .filter { layerOf(it) in forbidden }
                .map { "${source.path} -> ${it.removePrefix("$ROOT.")}" }
        }.toSet()
    }

    private fun sources(): List<Source> = SOURCE_SETS
        .map { File(it) }
        .filter { it.isDirectory }
        .flatMap { root -> root.walkTopDown().filter { it.extension == "kt" }.map { root to it } }
        .map { (root, file) -> Source.of(file.relativeTo(root).invariantSeparatorsPath, file.readLines()) }

    private fun layerOf(name: String?): String? =
        name?.takeIf { it.startsWith("$ROOT.") }?.removePrefix("$ROOT.")?.substringBefore('.')

    /** Пакет импорта: самый длинный известный пакет, с которого он начинается. */
    private fun packageOf(import: String, packages: Set<String>): String =
        packages.filter { import.startsWith("$it.") }.maxByOrNull { it.length } ?: import

    private fun debt(): Set<String> = javaClass.getResource("/layer-debt.txt")!!.readText()
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") }
        .toSet()

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
        val FORBIDDEN = mapOf(
            "domain" to setOf("data", "presentation"),
            "presentation" to setOf("data"),
            "data" to setOf("presentation")
        )
    }
}
