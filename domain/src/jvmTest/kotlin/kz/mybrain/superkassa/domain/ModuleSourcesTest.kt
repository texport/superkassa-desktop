package kz.mybrain.superkassa.domain

import kz.mybrain.superkassa.SourceTree
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Устройство модуля по его исходникам: чистый Kotlin и только домен.
 *
 * Компилятор держит зависимости — модулю не видны ни экраны, ни данные, ни
 * интеграции. Он не заметит другого: платформенного набора, который открыл
 * бы домену `java.*`, файла чужого слоя, положенного сюда по ошибке,
 * и открытого объявления без описания. Проверка читает исходники, поэтому
 * живёт в наборе проверок JVM — чтение файлов есть только там.
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

    /** Каждое открытое объявление верхнего уровня описано: домен читают снаружи. */
    @Test
    fun `открытые объявления описаны`() {
        File("src/commonMain/kotlin").walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val lines = file.readLines()
            lines.forEachIndexed { at, line ->
                if (DECLARATION.matches(line)) {
                    val above = lines.subList(0, at).lastOrNull { it.isNotBlank() && !it.trimStart().startsWith("@") }
                    assertTrue(above?.trim()?.endsWith("*/") == true, "${file.name}:${at + 1} без описания: $line")
                }
            }
        }
    }

    private companion object {
        /** Открытое объявление верхнего уровня: без `internal` и `private`. */
        val DECLARATION = Regex(
            """^((data|enum|sealed|abstract|open|value|fun|const|suspend|operator|inline) )*""" +
                """(class|object|interface|fun|typealias|val)\b.*"""
        )
    }
}
