package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.app.AppVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Разбор и сравнение версий кассы.
 *
 * Сравнение строками звало бы владельца с 1.0.10 «обновиться» до 1.0.9,
 * а метка GitHub с буквой `v` читалась бы чужой версией.
 */
class AppVersionTest {

    @Test
    fun `десятая правка новее девятой`() {
        val older = AppVersion.parse("1.0.9")!!
        val newer = AppVersion.parse("1.0.10")!!
        assertTrue(newer > older, "1.0.10 прочитана старше 1.0.9")
        assertTrue(AppVersion.parse("2.0.0")!! > AppVersion.parse("1.9.9")!!)
        assertTrue(AppVersion.parse("1.1.0")!! > AppVersion.parse("1.0.99")!!)
    }

    @Test
    fun `метка выпуска с буквой v — та же версия`() {
        assertEquals(AppVersion(1, 0, 3), AppVersion.parse("v1.0.3"))
        assertEquals(AppVersion.parse("1.0.3"), AppVersion.parse("v1.0.3"))
        assertEquals("1.0.3", AppVersion.parse("v1.0.3").toString())
    }

    @Test
    fun `сборка разработчика старше выпуска с теми же числами`() {
        val development = AppVersion.parse("1.0.0-dev")!!
        assertTrue(development.development)
        assertEquals("1.0.0-dev", development.label)
        assertTrue(AppVersion.parse("1.0.0")!! > development, "выпуск 1.0.0 не новее сборки 1.0.0-dev")
        assertTrue(development > AppVersion.parse("0.9.9")!!)
    }

    @Test
    fun `не версия не читается версией`() {
        assertNull(AppVersion.parse("nightly"))
        assertNull(AppVersion.parse("1.0"))
        assertNull(AppVersion.parse("1.0.x"))
        assertNull(AppVersion.parse(""))
        assertNull(AppVersion.parse(null))
    }
}
