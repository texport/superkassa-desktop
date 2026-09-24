package kz.mybrain.superkassa

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Границы слоёв внутри этого модуля: кто кого не видит.
 *
 * Домен — свой модуль, и что он никого не знает, держит граф модулей.
 * Здесь остались экраны и данные:
 *
 * - `presentation` не знает `data`;
 * - `data` не знает `presentation`;
 * - файлов домена в модуле нет — они живут в `domain`, и положенный сюда
 *   обходил бы границу модуля.
 *
 * Связывает слои только точка сборки — `Assembly.kt` настольной кассы
 * и `SuperkassaApp` Android, — и её эта проверка не касается.
 *
 * Нарушения, с которыми код пришёл в три слоя, перечислены в
 * `layer-debt.txt`: это долг, и он снимается при переводе своей области.
 * Новое нарушение падает сразу, а исправленное требует вычеркнуть строку
 * из списка — иначе список перестал бы говорить правду.
 */
class LayerBoundariesTest {

    @Test
    fun `no layer imports a layer it must not know`() {
        val fresh = violations() - SourceTree.debt(DEBT)
        assertTrue(fresh.isEmpty(), "new layer violations:\n" + fresh.sorted().joinToString("\n"))
    }

    @Test
    fun `debt lists only violations that still exist`() {
        val paid = SourceTree.debt(DEBT) - violations()
        assertEquals(emptySet(), paid, "violations fixed, remove them from $DEBT")
    }

    @Test
    fun `domain lives in its own module`() {
        val strays = SourceTree.main().filter { SourceTree.layerOf(it.pkg) == "domain" }.map { it.path }
        assertEquals(emptyList(), strays, "domain sources outside the domain module")
    }

    private fun violations(): Set<String> = SourceTree.main().flatMap { source ->
        val forbidden = FORBIDDEN[SourceTree.layerOf(source.pkg)].orEmpty()
        source.imports
            .filter { SourceTree.layerOf(it) in forbidden }
            .map { "${source.path} -> ${it.removePrefix("${SourceTree.ROOT}.")}" }
    }.toSet()

    private companion object {
        const val DEBT = "layer-debt.txt"
        val FORBIDDEN = mapOf(
            "presentation" to setOf("data"),
            "data" to setOf("presentation")
        )
    }
}
