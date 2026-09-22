package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.desktop.ui.settings.NodeAddressCard
import kz.mybrain.superkassa.desktop.ui.settings.SettingsDrafts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Набранное в настройках переживает уход в другой раздел.
 *
 * Экран настроек уходит из состава вместе с разделом: владелец набирал
 * адрес узла, отвлекался на журнал и возвращался к прежнему адресу,
 * ничего об этом не узнав. Сохранённое на диск при этом не менялось —
 * терялась именно работа рук.
 */
class SettingsDraftTest {

    @AfterTest
    fun forget() = SettingsDrafts.Field.NODE_ADDRESS.let(SettingsDrafts::forget)

    @Test
    fun `набранный адрес узла остаётся после ухода с экрана`() {
        val session = KassaScene.session("settings-draft")
        val saved = session.preferences.nodeUrl

        RenderProbe(width = WIDTH, height = HEIGHT) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.padding(Spacing.screen)) { NodeAddressCard(session) }
            }
        }.use { probe ->
            probe.frame()
            probe.click(FIELD)
            probe.type(TYPED)
        }

        // Экран ушёл вместе с разделом — набранное осталось.
        val draft = SettingsDrafts.of(SettingsDrafts.Field.NODE_ADDRESS, saved)
        assertNotEquals(saved, draft, "набранное в поле адреса не пережило уход с экрана")
        assertTrue(draft.contains(TYPED), "в поле осталось не то, что набрали: $draft")
        assertEquals(saved, session.preferences.nodeUrl, "адрес ушёл в настройки без нажатия «Сохранить»")
    }

    @Test
    fun `сохранённое перестаёт быть черновиком`() {
        SettingsDrafts.type(SettingsDrafts.Field.NODE_ADDRESS, "http://192.168.50.35:17700")
        SettingsDrafts.forget(SettingsDrafts.Field.NODE_ADDRESS)

        assertEquals(
            "http://127.0.0.1:8080",
            SettingsDrafts.of(SettingsDrafts.Field.NODE_ADDRESS, "http://127.0.0.1:8080"),
            "поле держится за черновик после сохранения"
        )
    }

    private companion object {
        const val WIDTH = 900
        const val HEIGHT = 300

        /** Куда нажать: поле адреса стоит первым в карточке. */
        val FIELD = Offset(150f, 100f)

        const val TYPED = "7"
    }
}
