package kz.mybrain.superkassa.domain

import kz.mybrain.superkassa.SourceTree
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Устройство модуля по его исходникам: чистый Kotlin и только домен.
 *
 * Компилятор держит зависимости — модулю не видны ни экраны, ни данные, ни
 * интеграции. Он не заметит другого: платформенного набора, который открыл
 * бы домену `java.*`, файла чужого слоя, положенного сюда по ошибке,
 * открытого объявления без описания и пакета, куда свалены сценарии области.
 * Проверка читает исходники, поэтому живёт в наборе проверок JVM — чтение
 * файлов есть только там.
 */
class ModuleSourcesTest {

    @Test
    fun `домен целиком в общем коде`() {
        assertEquals(listOf("commonMain"), SourceTree.mainSets(), "у домена появился платформенный набор")
    }

    @Test
    fun `в модуле только пакеты домена`() {
        val strangers = SourceTree.main().filter { SourceTree.layerOf(it.pkg) != "domain" }.map { it.path }
        assertEquals(emptyList(), strangers, "файлы чужого слоя в модуле домена")
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
