package kz.mybrain.superkassa.data.local.workplace

import kotlinx.io.files.Path
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Выбранная касса обязана пережить перезапуск целиком: усечённый
 * идентификатор не совпадёт ни с одной кассой, и кассир каждое утро
 * искал бы свою заново.
 */
class PreferencesTest {

    private val home: File = Files.createTempDirectory("prefs").toFile()

    private fun preferences() = Preferences(Path(home.path))

    @Test
    fun `касса запоминается и читается тем же значением`() {
        val id = "4467c6f4-9366-4eda-a47d-247ec46d9c9a"

        preferences().rememberedKkmId = id

        assertEquals(id, preferences().rememberedKkmId)

        val other = "0f2d1a7c-1111-2222-3333-444455556666"
        preferences().rememberedKkmId = other
        assertEquals(other, preferences().rememberedKkmId, "перезапись не должна оставлять огрызок")

        preferences().rememberedKkmId = null
        assertEquals(null, preferences().rememberedKkmId)
        home.deleteRecursively()
    }

    /**
     * Свёрнутая колонка точек переживает перезапуск так же, как рельс:
     * владелец сворачивает её, когда работает с одной кассой, и каждое
     * утро повторять это нажатие не должен.
     */
    @Test
    fun `свёрнутая колонка точек запоминается`() {
        assertEquals(false, preferences().look.placesCollapsed, "по умолчанию колонка развёрнута")

        preferences().let { it.look = it.look.copy(placesCollapsed = true) }
        assertEquals(true, preferences().look.placesCollapsed)

        preferences().let { it.look = it.look.copy(placesCollapsed = false) }
        assertEquals(false, preferences().look.placesCollapsed)
        home.deleteRecursively()
    }

    /**
     * Файлы на диске — те же, что писала касса до переноса настроек
     * в общий код: владелец обновляется и находит свою кассу, свой вид
     * окна и свой принтер там, где оставил. Файлы здесь записаны мимо
     * настроек, так, как они лежат у владельца.
     */
    @Test
    fun `настройки прежней кассы читаются как прежде`() {
        OLD_FILES.forEach { (name, text) -> File(home, name).apply { parentFile.mkdirs() }.writeText(text) }

        val read = preferences()
        assertEquals("4467c6f4", read.rememberedKkmId)
        assertEquals("TAXI", read.domain("4467c6f4"))
        assertEquals("Касса у входа", read.localName("4467c6f4"))
        assertEquals("kk", read.look.language)
        assertEquals(true, read.look.railCollapsed)
        assertEquals(1440 to 900, read.windowSize)
        assertEquals(setOf("buyer", "payment"), read.collapsedPanels)
        assertEquals(true, read.locationAllowed)
        assertEquals("XP-80", read.printing.printer("4467c6f4"))
        assertEquals(2, read.printing.copies)
        assertEquals("environment-value", read.setupValue("environment"))
        home.deleteRecursively()
    }

    /** Записанное настройками ложится в те же файлы и тем же текстом, что читала прежняя касса. */
    @Test
    fun `настройки пишутся в прежние файлы`() {
        preferences().apply {
            rememberedKkmId = "4467c6f4"
            chooseDomain("4467c6f4", "TAXI")
            rename("4467c6f4", "Касса у входа")
            look = look.copy(language = "kk", railCollapsed = true)
            windowSize = 1440 to 900
            collapsedPanels = setOf("buyer", "payment")
            locationAllowed = true
            printing.choosePrinter("4467c6f4", "XP-80")
            printing.copies = 2
            setupValue("environment", "environment-value")
        }

        OLD_FILES.forEach { (name, text) -> assertEquals(text, File(home, name).readText(), name) }
        home.deleteRecursively()
    }

    private companion object {
        /** Файлы рабочего места и их содержимое — как их пишет касса. */
        val OLD_FILES = listOf(
            "kkm" to "4467c6f4",
            "domains/4467c6f4" to "TAXI",
            "names/4467c6f4" to "Касса у входа",
            "language" to "kk",
            "rail" to "collapsed",
            "window" to "1440x900",
            "panels" to "buyer,payment",
            "location" to "allowed",
            "printers/4467c6f4" to "XP-80",
            "print-copies" to "2",
            "setup/environment" to "environment-value"
        )
    }
}
