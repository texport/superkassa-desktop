package kz.mybrain.superkassa.navigation

import kz.mybrain.superkassa.SourceTree
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Устройство модуля по исходникам: только ключи и переход, открытое описано.
 *
 * Навигацию берут все области, и она не должна знать ни одной из них:
 * ни экранов, ни домена, ни оформления — иначе область через неё увидела бы
 * соседку.
 */
class ModuleSourcesTest {

    @Test
    fun `в модуле только пакеты навигации`() {
        val strangers = SourceTree.main().filter { SourceTree.layerOf(it.pkg) != LAYER }.map { it.path }
        assertEquals(emptyList(), strangers, "файлы чужого слоя в навигации")
    }

    @Test
    fun `навигация не берёт кода приложения`() {
        val foreign = SourceTree.main().flatMap { source ->
            source.imports.filterNot { it.startsWith("${SourceTree.ROOT}.$LAYER.") }.map { "${source.path} -> $it" }
        }
        assertEquals(emptyList(), foreign, "навигация видит код приложения")
    }

    @Test
    fun `открытые объявления описаны`() {
        assertEquals(emptyList(), SourceTree.undocumented(), "открытые объявления без описания")
    }

    @Test
    fun `в пакете не больше пятнадцати файлов`() {
        assertEquals(emptyList(), SourceTree.crowded(), "пакеты, которые пора разложить по сценариям")
    }

    private companion object {
        const val LAYER = "navigation"
    }
}
