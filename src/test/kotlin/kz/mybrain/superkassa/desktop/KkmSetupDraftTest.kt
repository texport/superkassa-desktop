package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.app.KkmSetupDraft
import kz.mybrain.superkassa.desktop.app.Preferences
import kz.mybrain.superkassa.desktop.app.SetupStep
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Незаконченное подключение кассы переживает закрытие приложения.
 *
 * Проверка не про удобство: заводской номер узел выдаёт новым на каждый
 * запрос, и потерянный черновик означает, что в кабинет унесли один
 * номер, а кассу завели с другим.
 */
class KkmSetupDraftTest {

    private fun preferences(): Preferences {
        val home = Files.createTempDirectory("superkassa-draft").toFile()
        return Preferences(File(home, "settings"))
    }

    @Test
    fun `номер и год переживают перезапуск`() {
        val preferences = preferences()
        KkmSetupDraft(preferences).rememberFactory("KZT26088C012846", "2026")

        val reopened = KkmSetupDraft(preferences)
        assertEquals("KZT26088C012846", reopened.factoryNumber)
        assertEquals("2026", reopened.manufactureYear)
    }

    @Test
    fun `мастер открывается там, где его оставили`() {
        val preferences = preferences()
        val draft = KkmSetupDraft(preferences)
        assertEquals(SetupStep.Factory, draft.step())

        draft.rememberFactory("KZT26088C012846", "2026")
        assertEquals(SetupStep.Cabinet, draft.step())

        draft.rememberRegister("22222222-2222-2222-2222-222222222222", 5000004)
        assertEquals(SetupStep.Application, draft.step())
    }

    @Test
    fun `идентификатор ОФД запоминается вместе с кассой кабинета`() {
        val preferences = preferences()
        KkmSetupDraft(preferences).rememberRegister("33333333-3333-3333-3333-333333333333", 5000004)
        assertEquals("5000004", KkmSetupDraft(preferences).systemId)
    }

    @Test
    fun `подключённая касса забывается вместе с черновиком`() {
        val preferences = preferences()
        val draft = KkmSetupDraft(preferences)
        draft.rememberFactory("KZT26088C012846", "2026")
        draft.rememberRegister("44444444-4444-4444-4444-444444444444", 5000004)

        draft.clear()

        val reopened = KkmSetupDraft(preferences)
        assertNull(reopened.factoryNumber)
        assertNull(reopened.cabinetRegisterId)
        assertNull(reopened.systemId)
        assertEquals(SetupStep.Factory, reopened.step())
    }
}
