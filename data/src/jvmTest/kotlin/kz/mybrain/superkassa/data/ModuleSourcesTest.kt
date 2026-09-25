package kz.mybrain.superkassa.data

import kz.mybrain.superkassa.SourceTree
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Устройство модуля по его исходникам: только слой данных, открытое описано.
 *
 * Что данные не видят экранов, держит граф модулей. Он не заметит файла
 * чужого слоя, положенного сюда по ошибке, и открытого объявления без
 * описания: адаптеры собирают точки сборки, и читают их снаружи, не
 * открывая модуля.
 */
class ModuleSourcesTest {

    @Test
    fun `в модуле только пакеты данных`() {
        val strangers = SourceTree.main().filter { SourceTree.layerOf(it.pkg) != "data" }.map { it.path }
        assertEquals(emptyList(), strangers, "файлы чужого слоя в модуле данных")
    }

    @Test
    fun `в пакете не больше пятнадцати файлов`() {
        assertEquals(emptyList(), SourceTree.crowded(), "пакеты, которые пора разложить по сценариям")
    }

    @Test
    fun `открытые объявления описаны`() {
        assertEquals(emptyList(), SourceTree.undocumented(), "открытые объявления без описания")
    }
}
