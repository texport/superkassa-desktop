package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.app.Preferences
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

        Preferences(file).defaultKkmId = id

        assertEquals(id, Preferences(file).defaultKkmId)

        val other = "0f2d1a7c-1111-2222-3333-444455556666"
        Preferences(file).defaultKkmId = other
        assertEquals(other, Preferences(file).defaultKkmId, "перезапись не должна оставлять огрызок")

        Preferences(file).defaultKkmId = null
        assertEquals(null, Preferences(file).defaultKkmId)
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

        assertEquals(false, Preferences(file).placesCollapsed, "по умолчанию колонка развёрнута")

        Preferences(file).placesCollapsed = true
        assertEquals(true, Preferences(file).placesCollapsed)

        Preferences(file).placesCollapsed = false
        assertEquals(false, Preferences(file).placesCollapsed)
        home.deleteRecursively()
    }
}
