package kz.mybrain.superkassa.presentation.settings.ofd

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Замена токена ОФД на телефоне 360×800: поле и кнопка не выходят за край.
 *
 * Поле стояло постоянной ширины рядом с кнопкой, и на телефоне кнопка
 * «Записать» уезжала за край карточки. Кадр — `/tmp/narrow-ofd-token.png`.
 */
class OfdTokenNarrowTest {

    @Test
    fun `поле токена и кнопка видны целиком на телефоне`() {
        val texts = textsOf(Language.Ru).common.settingsScreen
        val state = OfdSettingsUiState(kkm = KassaScene.kkm(state = PROGRAMMING), token = "123456789012")
        RenderProbe(WIDTH, HEIGHT) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.padding(Spacing.fieldGap)) { OfdTokenCard(state, object : OfdSettingsActions {}) }
            }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            File("/tmp/narrow-ofd-token.png").writeBytes(probe.frame())
            val nodes = probe.nodes()
            val field = nodes.first { it.editable }
            val save = nodes.first { it.text == texts.saveToken }
            listOf(field, save).forEach { node ->
                assertTrue(node.at.x + node.width <= WIDTH && node.whole, "за краем экрана: $node")
            }
        }
    }

    private companion object {
        const val WIDTH = 360
        const val HEIGHT = 800
        const val SETTLE = 5
        const val PROGRAMMING = "PROGRAMMING"
    }
}
