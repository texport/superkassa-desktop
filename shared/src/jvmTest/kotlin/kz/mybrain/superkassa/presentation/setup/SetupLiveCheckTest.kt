package kz.mybrain.superkassa.presentation.setup

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.awaitCancellation
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.setup.model.CabinetRecord
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.presentation.cabinet.applications.LocalSignTick
import kz.mybrain.superkassa.presentation.setup.component.ApplicationStepCard
import kz.mybrain.superkassa.presentation.setup.registration.RegistrationViewModel
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.tap
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

/**
 * Что нашла живая проверка на шаге постановки на учёт.
 *
 * Отсчёт подписи показывал «Осталось %1$s»: номерное место шаблона
 * не заполнялось. Пока заявление «На рассмотрении в КГД», главной
 * и живой оставалась «Подать заявление». Кадры — `/tmp/live-check-*.png`.
 */
class SetupLiveCheckTest {

    @Test
    fun `отсчёт подписи называет срок, а не знак подстановки`(): Unit = inlineMain {
        val scene = SetupScene().started(halfway = true)
        scene.cabinet.signer = { awaitCancellation() }
        val model = scene.registration()
        RenderProbe(CARD, TALL) { Step(model, scene) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.tap { it.text == scene.texts.submit }
            repeat(SETTLE) { probe.frame() }
            File("/tmp/live-check-sign-wait.png").writeBytes(probe.frame())
            val texts = probe.nodes().map { it.text }

            assertTrue(model.state.value.signing, "подпись не попрошена")
            assertFalse(texts.any { "%" in it }, "на экране знак подстановки: $texts")
            model.cancelSubmit()
        }
    }

    @Test
    fun `пока заявление у КГД, главное действие — обновить, а подачи нет`(): Unit = inlineMain {
        val draft = frame("draft", CabinetRecord("DRAFT", null))
        val awaiting = frame("awaiting", CabinetRecord(IN_KGD, null, awaiting = true))
        val refresh = textsOf(Language.Ru).cabinet.refresh
        val submit = SetupScene().texts.submit

        assertTrue(submit in draft, "черновику не предложено подать заявление")
        assertFalse(submit in awaiting, "поданное заявление предлагают подать снова")
        assertTrue(refresh in awaiting, "поданное заявление нечем перечитать")
    }

    /** Надписи шага на экране; кадр — в файл. */
    private fun frame(name: String, record: CabinetRecord): List<String> = inlineMain {
        val scene = SetupScene().started(halfway = true)
        scene.cabinet.record = record
        val model = scene.registration()
        RenderProbe(CARD, TALL) { Step(model, scene) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            File("/tmp/live-check-application-$name.png").writeBytes(probe.frame())
            probe.nodes().map { it.text }
        }
    }

    /** Шаг постановки на учёт так, как его собирает мастер: касса в кабинете прочитана. */
    @Composable
    private fun Step(model: RegistrationViewModel, scene: SetupScene) {
        val state by model.state.collectAsState()
        LaunchedEffect(Unit) { model.readRecord(REGISTER) }
        CompositionLocalProvider(LocalSignTick provides TICK) {
            Column(modifier = Modifier.fillMaxWidth().padding(Spacing.fieldGap)) {
                ApplicationStepCard(REGISTER, state, model, scene.texts, open = true, busy = false)
            }
        }
    }

    private companion object {
        const val CARD = 720
        const val TALL = 420
        const val SETTLE = 30
        const val REGISTER = "r-1"
        const val IN_KGD = "REGISTRATION_IN_ISNA_PROCESS"
        val TICK = 50.milliseconds
    }
}
