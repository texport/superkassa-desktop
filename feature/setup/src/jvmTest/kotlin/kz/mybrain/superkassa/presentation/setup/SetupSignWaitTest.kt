package kz.mybrain.superkassa.presentation.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.awaitCancellation
import kz.mybrain.superkassa.CabinetStepsRig
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.presentation.common.cabinet.CabinetSession
import kz.mybrain.superkassa.presentation.common.cabinet.CabinetSteps
import kz.mybrain.superkassa.presentation.setup.component.ApplicationStepCard
import kz.mybrain.superkassa.presentation.setup.registration.RegistrationViewModel
import kz.mybrain.superkassa.tap
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * Шаг постановки на учёт в мастере: состояние словами и ожидание подписи.
 *
 * Видимый срок с отменой стоял только на двери входа, а подпись приложение
 * просит и в мастере: там владелец смотрел на занятую кнопку. Состояние
 * кассы в кабинете приходит кодом, и на шаге стояло «Состояние кассы: DRAFT».
 */
class SetupSignWaitTest {

    @Test
    fun `состояние кассы в кабинете названо словами`(): Unit = inlineMain {
        val scene = SetupScene().started(halfway = true)
        val model = scene.registration()
        val steps = CabinetStepsRig.idle()
        RenderProbe(CARD, TALL) { Step(model, scene, steps) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            File("/tmp/fix-13-draft-in-words.png").writeBytes(probe.frame())
            val status = probe.nodes().map { it.text }.single { it.startsWith(scene.texts.status) }

            assertFalse("DRAFT" in status, "состояние кассы названо кодом: $status")
        }
    }

    @Test
    fun `мастер показывает ожидание подписи и снимает его отменой`(): Unit = inlineMain {
        val scene = SetupScene().started(halfway = true)
        scene.cabinet.signer = { awaitCancellation() }
        val model = scene.registration()
        val steps = CabinetStepsRig.idle()
        RenderProbe(CARD, TALL) { Step(model, scene, steps) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            val before = probe.frame()
            probe.tap { it.text == scene.texts.submit }
            val waiting = probe.frame()
            File("/tmp/fix-15-setup-wait.png").writeBytes(waiting)

            assertTrue(model.state.value.signing, "подпись не попрошена")
            assertFalse(waiting.contentEquals(before), "ожидание подписи в мастере не показано")
            Thread.sleep(MOVED.inWholeMilliseconds)
            assertTrue(probe.changedFrom(waiting), "отсчёт в мастере не двигается")

            model.cancelSubmit()
            repeat(SETTLE) { probe.frame() }
            assertFalse(model.state.value.signing, "отсчёт остался после отмены")
            assertTrue(scene.cabinet.asked.none { it.startsWith("send") }, "прерванная подача отправлена")
        }
    }

    /** Шаг постановки на учёт так, как его собирает мастер: касса в кабинете прочитана. */
    @Composable
    private fun Step(model: RegistrationViewModel, scene: SetupScene, steps: CabinetSteps) {
        val state by model.state.collectAsState()
        LaunchedEffect(Unit) { model.readRecord(REGISTER) }
        CabinetStepsRig.Ticking(TICK) {
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.fieldGap)) {
                ApplicationStepCard(REGISTER, state, model, scene.texts, steps, CabinetSession(open = true))
            }
        }
    }

    private companion object {
        const val CARD = 720
        const val TALL = 420
        const val SETTLE = 30

        /** Касса в кабинете из пройденного сцены. */
        const val REGISTER = "r-1"

        /** Шаг отсчёта в проверке: секунда не ловится под нагрузкой соседних прогонов. */
        val TICK = 50.milliseconds

        /** Сколько проверка ждёт движения отсчёта: несколько его шагов с запасом. */
        val MOVED = 400.milliseconds
    }
}
