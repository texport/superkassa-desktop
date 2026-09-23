package kz.mybrain.superkassa.kassa

import kz.mybrain.superkassa.data.local.DataHome
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Каталог данных: разработка не трогает кассу, работающую на той же машине.
 *
 * Запуск из исходников однажды затёр настройки настоящей кассы: у выпуска
 * и у сборки разработчика был один каталог.
 */
class DataHomeTest {

    @Test
    fun `сборка разработчика живёт в своём каталоге`() {
        assertEquals(File("/home/k", ".superkassa-dev"), DataHome.resolve(null, null, "1.0.0-dev", "/home/k"))
    }

    @Test
    fun `выпуск живёт в каталоге кассы`() {
        assertEquals(File("/home/k", ".superkassa"), DataHome.resolve(null, "", "1.4.0", "/home/k"))
    }

    @Test
    fun `названный каталог сильнее вида сборки, переменная — сильнее свойства`() {
        assertEquals(File("/data/a"), DataHome.resolve("/data/a", "/data/b", "1.4.0", "/home/k"))
        assertEquals(File("/data/b"), DataHome.resolve(" ", "/data/b", "1.0.0-dev", "/home/k"))
    }
}
