package kz.mybrain.superkassa.presentation.cabinet.applications

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.CabinetRig
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.data.eds.NcaFake
import kz.mybrain.superkassa.data.eds.NcaReply
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.presentation.cabinet.signingRig
import kz.mybrain.superkassa.presentation.cabinet.viewOf
import kz.mybrain.superkassa.presentation.common.model.ProvideWindowModels
import kz.mybrain.superkassa.presentation.common.model.WindowModels
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.tap
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Ожидание подписи при подаче заявления из кабинета.
 *
 * Видимый срок с отменой стоял только на двери входа, а подпись приложение
 * просит в трёх местах: в двух остальных владелец смотрел на занятую кнопку.
 * Что отменённая подача ничего не отправляет в кабинет, проверяет
 * [ApplicationViewModelTest].
 */
class ApplicationSignWaitTest {

    private val texts = textsOf(Language.Ru).cabinet

    /** Куда дошли обращения к кабинету: по ним видно, ушло ли подписанное. */
    private val asked = CopyOnWriteArrayList<String>()

    /** Подача из кабинета: на месте кнопки отсчёт срока и отмена. */
    @Test
    fun `подача из кабинета показывает ожидание подписи`(): Unit = inlineMain {
        NcaFake { NcaReply.Silence }.use { fake ->
            val cabinet = signingRig(fake, PREPARED, asked).enter()
            RenderProbe(CARD, TALL) { Actions(cabinet) }.use { probe ->
                val before = probe.frame()
                probe.tap { it.text == texts.submitApplication }
                val waiting = probe.frame()
                File("/tmp/fix-15-cabinet-wait.png").writeBytes(waiting)

                assertFalse(waiting.contentEquals(before), "ожидание подписи на экране не показано")
                Thread.sleep(MOVED.inWholeMilliseconds)
                assertTrue(probe.changedFrom(waiting), "отсчёт не двигается")

                // Отмена возвращает кнопку подачи: оставленный отсчёт значил бы,
                // что владелец ждёт того, чего никто уже не делает.
                probe.tap { it.text == textsOf(Language.Ru).cabinet.eds.cancelWait }
                val cancelled = probe.frame()
                File("/tmp/fix-15-cabinet-cancelled.png").writeBytes(cancelled)
                assertFalse(cancelled.contentEquals(waiting), "отсчёт остался на экране после отмены")
                // Отмена доходит до ожидания подписи своим чередом: занятость
                // снимается, когда оно действительно прервано.
                val until = System.nanoTime() + WAIT.inWholeNanoseconds
                while (cabinet.model.state.value.busy && System.nanoTime() < until) {
                    Thread.sleep(STEP.inWholeMilliseconds)
                }
                assertFalse(cabinet.model.state.value.busy, "после отмены приложение осталось занятым")
            }
        }
    }

    /** Действия кассы-черновика в кабинете: кнопка подачи и её ожидание. */
    @Composable
    private fun Actions(cabinet: CabinetRig) {
        CompositionLocalProvider(LocalSignTick provides TICK) {
            ProvideWindowModels(remember { WindowModels() }) {
                Column(modifier = Modifier.fillMaxWidth().padding(Spacing.fieldGap)) {
                    RegistrationActionsBlock(cabinet.model, Language.Ru, texts, viewOf(draft())) {}
                }
            }
        }
    }

    /** Касса-черновик: ей подаётся постановка на учёт. */
    private fun draft() = CabinetRegister(
        id = "r-1",
        kkmId = 5_000_021,
        internalName = "Касса у входа",
        status = "DRAFT",
        factoryNumber = "SN-ECC-172758"
    )

    private companion object {
        const val PREPARED = """{"actionId":"a-1","actionType":"REGISTRATION","payloadToSign":"cGF5bG9hZA=="}"""

        const val CARD = 720
        const val TALL = 420
        const val SETTLE = 30

        /** Шаг отсчёта в проверке: секунда не ловится под нагрузкой соседних прогонов. */
        val TICK = 50.milliseconds

        /** Сколько проверка ждёт движения отсчёта: несколько его шагов с запасом. */
        val MOVED = 400.milliseconds
        val WAIT = 10.seconds
        val STEP = 20.milliseconds
    }
}
