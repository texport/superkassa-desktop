package kz.mybrain.superkassa.presentation.cabinet

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.Windowed
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.mockCabinet
import kz.mybrain.superkassa.presentation.cabinet.enroll.AddRegisterDialog
import kz.mybrain.superkassa.presentation.cabinet.places.AddPlaceCard
import kz.mybrain.superkassa.presentation.cabinet.register.adopt.AdoptRegisterDialog
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Окна заведения кабинета показывают свои поля.
 *
 * Поля окна прокручивает само окно формы. Вложенная в него вторая
 * прокрутка с весом получала от прокручиваемого окна нулевую высоту,
 * и окна заведения точки, кассы и переноса кассы на машину выходили
 * с заголовком и кнопками, но без единого поля: заполнить их было нечем.
 */
class CabinetFormFieldsTest {

    private val texts = textsOf(Language.Ru).cabinet

    @Test
    fun `окно заведения точки показывает поля`() = fieldsShown("точки") {
        AddPlaceCard(mockCabinet(EMPTY), texts, onDismiss = {}, onAdded = {})
    }

    @Test
    fun `окно заведения кассы показывает поля`() = fieldsShown("кассы") {
        AddRegisterDialog(mockCabinet(EMPTY), texts, onDismiss = {}, onAdded = {})
    }

    @Test
    fun `окно переноса кассы на машину показывает поля`() = fieldsShown("переноса кассы") {
        val register = CabinetRegister(id = "r-1", kkmId = 5_000_021, status = "REGISTERED")
        AdoptRegisterDialog(mockCabinet(EMPTY), texts, viewOf(register), onDismiss = {})
    }

    /**
     * Без места на карте точку не создать: кабинет без широты и долготы
     * отвечает отказом без имён полей. Кнопка погашена, а под полем адреса
     * сказано, чего не хватает.
     */
    @Test
    fun `без места на карте точку не создать`() {
        RenderProbe(WIDTH, HEIGHT) {
            Windowed { AddPlaceCard(mockCabinet(EMPTY), texts, onDismiss = {}, onAdded = {}) }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            val hint = probe.nodes().any { it.text == texts.pointNotChosen }
            assertTrue(hint, "не сказано, что место на карте не выбрано")
            val action = probe.semantics().single { node ->
                val button = node.config.getOrNull(SemanticsProperties.Role) == Role.Button
                button && node.children.any { it.says(texts.addPlace) }
            }
            assertTrue(SemanticsProperties.Disabled in action.config, "кнопка создания доступна без места на карте")
        }
    }

    private fun SemanticsNode.says(text: String) = config.getOrNull(SemanticsProperties.Text)?.joinToString() == text

    private fun fieldsShown(form: String, dialog: @Composable () -> Unit) {
        RenderProbe(WIDTH, HEIGHT) { Windowed(dialog) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            val fields = probe.nodes().filter { it.editable && it.visible.height > 0 }
            assertTrue(fields.isNotEmpty(), "в окне заведения $form не видно ни одного поля")
        }
    }

    private companion object {
        const val WIDTH = 1180
        const val HEIGHT = 820
        const val SETTLE = 24
        const val EMPTY = """{"items":[]}"""
    }
}
