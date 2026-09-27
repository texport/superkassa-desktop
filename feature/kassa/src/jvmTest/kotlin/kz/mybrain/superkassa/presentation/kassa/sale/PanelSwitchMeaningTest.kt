package kz.mybrain.superkassa.presentation.kassa.sale

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Переключатель — сворачивание: включён у свёрнутого раздела.
 *
 * Владелец: «я включаю, а они развёрнуты». Когда включённый значил
 * «развёрнут», он включал его, чтобы свернуть.
 */
class PanelSwitchMeaningTest {
    private val texts = textsOf(Language.Ru).common.settingsScreen

    @Test
    fun `включён переключатель у свёрнутого раздела, выключен — у развёрнутого`() {
        val collapsed = setOf(SalePanel.Money)
        val states = RenderProbe(width = WIDE, height = TALL) {
            PanelBehaviourGroup(expanded = { it !in collapsed }, onToggle = {})
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            val titles = listOf(texts.panelMoney, texts.panelCustomerData)
            probe.nodes { all -> titles.map { title -> switchOf(all, title) } }
        }
        assertEquals(listOf(ToggleableState.On, ToggleableState.Off), states)
    }

    private fun switchOf(all: List<SemanticsNode>, title: String): ToggleableState? =
        all.first { it.config.contains(SemanticsProperties.ToggleableState) && it.says(title) }
            .config.getOrNull(SemanticsProperties.ToggleableState)

    private fun SemanticsNode.says(title: String): Boolean =
        config.getOrNull(SemanticsProperties.Text)?.any { it.text == title } == true || children.any { it.says(title) }

    private companion object {
        const val WIDE = 720
        const val TALL = 1200
        const val SETTLE = 10
    }
}
