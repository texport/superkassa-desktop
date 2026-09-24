package kz.mybrain.superkassa

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Границы слоёв: в этом модуле только экраны.
 *
 * Слои разведены по модулям: `domain` не видит никого, `data` видит только
 * домен, а этому модулю слой данных не виден вовсе — адаптеры собирают
 * точки сборки платформенных приложений. Импорты это держит граф модулей,
 * а не проверка.
 *
 * Граф не заметит одного: файла домена или данных, положенного сюда. Такой
 * файл обходил бы границу модуля — экраны видели бы его как свой, — и эта
 * проверка его не пропускает.
 */
class LayerBoundariesTest {

    @Test
    fun `only presentation lives in this module`() {
        val strays = SourceTree.main().filter { SourceTree.layerOf(it.pkg) != "presentation" }.map { it.path }
        assertEquals(emptyList(), strays, "sources of another layer in the presentation module")
    }
}
