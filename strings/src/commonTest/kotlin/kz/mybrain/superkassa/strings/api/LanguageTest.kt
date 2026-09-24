package kz.mybrain.superkassa.strings.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Выбор языка и слов на нём.
 *
 * Язык выбирает владелец; не выбран — язык системы; система чужого языка —
 * государственный. Слова, пришедшие сразу на трёх языках, берутся на языке
 * кассира, а пустое подменяется русским.
 */
class LanguageTest {

    @Test
    fun `выбранный язык важнее языка системы`() {
        assertEquals(Language.Kk, Language.byCode(code = "kk", system = "ru"))
        assertEquals(Language.En, Language.byCode(code = "en", system = null))
    }

    @Test
    fun `без выбора язык системы а при чужой системе государственный`() {
        assertEquals(Language.Ru, Language.byCode(code = null, system = "ru"))
        assertEquals(Language.Kk, Language.byCode(code = null, system = "fr"))
        assertEquals(Language.Kk, Language.byCode(code = "xx", system = null))
    }

    @Test
    fun `слова на языке кассира а пустое по-русски`() {
        assertEquals("Сатылым", Language.Kk.choose("Продажа", "Сатылым", "Sale"))
        assertEquals("Продажа", Language.En.choose("Продажа", "Сатылым", " "))
        assertNull(Language.Kk.choose(" ", "", ""))
    }

    @Test
    fun `тексты языка собраны один раз`() {
        Language.entries.forEach { assertSame(textsOf(it), textsOf(it)) }
    }
}
