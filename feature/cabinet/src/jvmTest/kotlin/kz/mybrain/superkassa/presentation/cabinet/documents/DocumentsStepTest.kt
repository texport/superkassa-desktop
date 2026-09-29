package kz.mybrain.superkassa.presentation.cabinet.documents

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.navigation3.runtime.NavKey
import kz.mybrain.superkassa.CabinetStage
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.StubReply
import kz.mybrain.superkassa.data.cabinet.CabinetBodies
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.navigation.LocalNavigator
import kz.mybrain.superkassa.navigation.Navigator
import kz.mybrain.superkassa.navigation.step.RegisterDocumentsKey
import kz.mybrain.superkassa.navigation.step.StepKey
import kz.mybrain.superkassa.presentation.cabinet.CabinetScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Документы кассы — шаг истории окна, а не наложение поверх кабинета.
 *
 * Наложением они были вне истории: системный «назад» уводил из документов
 * мимо карточки кассы на главный экран, а стрелка шапки вела к карточке.
 * Шагом «назад» у них один — по истории окна, и снятый шаг закрывает
 * документы, как бы владелец ни ушёл.
 */
class DocumentsStepTest {
    private val stage = CabinetStage { StubReply(CabinetBodies.NOTHING) }
    private val model = stage.cabinet.cabinet
    private val steps = mutableStateListOf<NavKey>()
    private val navigator = object : Navigator {
        override fun open(key: NavKey) {
            steps.add(key)
        }

        override fun back() {
            steps.removeLastOrNull()
        }

        override fun close(key: NavKey) {
            steps.remove(key)
        }
    }

    private fun scene(check: (RenderProbe) -> Unit) = RenderProbe(WIDE, HIGH) {
        CompositionLocalProvider(LocalNavigator provides navigator) {
            stage.Window { CabinetScreen(stage.cabinet, steps.lastOrNull() as? StepKey) }
        }
    }.use { probe ->
        repeat(SETTLE) { probe.frame() }
        check(probe)
    }

    @Test
    fun `снятый шаг закрывает документы`() = scene { probe ->
        model.view.openDocuments(REGISTER)
        navigator.open(RegisterDocumentsKey)
        repeat(SETTLE) { probe.frame() }
        assertEquals(REGISTER, model.state.value.documentsOf)

        navigator.back()
        repeat(SETTLE) { probe.frame() }

        assertNull(model.state.value.documentsOf, "шаг снят, а шапка всё ещё называет документы кассы")
    }

    @Test
    fun `шаг без кассы снимается сам`() = scene { probe ->
        navigator.open(RegisterDocumentsKey)
        repeat(SETTLE) { probe.frame() }

        assertTrue(steps.isEmpty(), "пустой шаг документов остался в истории окна")
    }

    private companion object {
        val REGISTER = CabinetRegister(id = "r-1", kkmId = 5_000_021, status = "REGISTERED")
        const val WIDE = 1180
        const val HIGH = 820
        const val SETTLE = 20
    }
}
