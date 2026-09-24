package kz.mybrain.superkassa.data.local

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Выбранная касса обязана пережить перезапуск целиком: усечённый
 * идентификатор не совпадёт ни с одной кассой, и кассир каждое утро
 * искал бы свою заново.
 */
class PreferencesTest {

    @Test
    fun `касса запоминается и читается тем же значением`() {
        val file = File.createTempFile("prefs", ".kkm").also { it.delete() }
        val id = "4467c6f4-9366-4eda-a47d-247ec46d9c9a"

        Preferences(file).rememberedKkmId = id

        assertEquals(id, Preferences(file).rememberedKkmId)

        val other = "0f2d1a7c-1111-2222-3333-444455556666"
        Preferences(file).rememberedKkmId = other
        assertEquals(other, Preferences(file).rememberedKkmId, "перезапись не должна оставлять огрызок")

        Preferences(file).rememberedKkmId = null
        assertEquals(null, Preferences(file).rememberedKkmId)
        file.delete()
    }

    /**
     * Свёрнутая колонка точек переживает перезапуск так же, как рельс:
     * владелец сворачивает её, когда работает с одной кассой, и каждое
     * утро повторять это нажатие не должен.
     */
    @Test
    fun `свёрнутая колонка точек запоминается`() {
        val home = File.createTempFile("places", "").also { it.delete() }
        home.mkdirs()
        val file = File(home, "kkm")

        assertEquals(false, Preferences(file).look.placesCollapsed, "по умолчанию колонка развёрнута")

        Preferences(file).let { it.look = it.look.copy(placesCollapsed = true) }
        assertEquals(true, Preferences(file).look.placesCollapsed)

        Preferences(file).let { it.look = it.look.copy(placesCollapsed = false) }
        assertEquals(false, Preferences(file).look.placesCollapsed)
        home.deleteRecursively()
    }
}
