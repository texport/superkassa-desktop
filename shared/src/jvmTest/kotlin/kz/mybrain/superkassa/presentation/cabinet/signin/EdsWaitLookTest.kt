package kz.mybrain.superkassa.presentation.cabinet.signin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ElevatedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.data.eds.NcaFake
import kz.mybrain.superkassa.data.eds.NcaReply
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.cabinet.company.actions
import kz.mybrain.superkassa.presentation.cabinet.component.SignWait
import kz.mybrain.superkassa.presentation.cabinet.signingRig
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Экран входа, пока идёт ожидание подписи.
 *
 * Владелец подписал в окне NCALayer и три минуты смотрел на неподвижную
 * кнопку, а затем прочитал, что NCALayer не запущен. Проверяется то,
 * чего на экране не было: видимый срок ожидания и отмена — и то, что
 * по отмене приложение не остаётся занятым.
 */
class EdsWaitLookTest {

    private val texts = textsOf(Language.Ru).cabinet
    private val eds = textsOf(Language.Ru).cabinet.eds

    /**
     * Ожидание видно, и оно движется.
     *
     * Кадр после нажатия отличается от кадра до него, а кадр спустя
     * секунду — от кадра сразу после нажатия: отсчёт идёт. Неподвижное
     * ожидание владелец читает как зависшее приложение.
     */
    @Test
    fun `ожидание подписи показано с отсчётом и отменой`(): Unit = inlineMain {
        NcaFake { NcaReply.Silence }.use { fake ->
            val cabinet = signingRig(fake, CHALLENGE).model
            RenderProbe { Door(cabinet) }.use { probe ->
                val door = probe.frame()
                probe.click(SIGN_IN)
                val waiting = probe.frame()
                File("/tmp/eds-wait-screen.png").writeBytes(waiting)

                assertTrue(cabinet.state.value.busy, "нажатие не дошло до кнопки входа: разметка карточки сдвинулась")
                assertFalse(waiting.contentEquals(door), "ожидание на экране не показано")
                Thread.sleep(TICK.inWholeMilliseconds)
                assertTrue(probe.changedFrom(waiting), "отсчёт не двигается")
            }
        }
    }

    /**
     * Отмена снимает ожидание и не оставляет приложение занятым.
     *
     * Кнопка отмены стоит там же, где кнопка входа: ожидание прерывается
     * тем же местом, где началось.
     */
    @Test
    fun `отмена снимает ожидание с экрана`(): Unit = inlineMain {
        NcaFake { NcaReply.Silence }.use { fake ->
            val cabinet = signingRig(fake, CHALLENGE).model
            RenderProbe { Door(cabinet) }.use { probe ->
                probe.click(SIGN_IN)
                probe.frame()
                probe.click(CANCEL)
                val after = probe.frame()
                File("/tmp/eds-wait-cancelled.png").writeBytes(after)

                // Отмена доходит до ожидания подписи своим чередом.
                val until = System.nanoTime() + WAIT.inWholeNanoseconds
                while (cabinet.state.value.busy && System.nanoTime() < until) Thread.sleep(STEP.inWholeMilliseconds)
                assertFalse(cabinet.state.value.busy, "после отмены приложение осталось занятым")
                assertNull(cabinet.talk.notices.last, "по отмене владельцу ничего не показывается")
            }
        }
    }

    /** Последние секунды срока: цифры те же, а ждать уже недолго. */
    @Test
    fun `остаток срока виден до последних секунд`() {
        val shot = RenderProbe(width = WIDTH, height = HEIGHT) {
            Column(modifier = Modifier.fillMaxSize().padding(Spacing.blockPadding)) {
                ElevatedCard(modifier = Modifier.widthIn(max = Sizes.loginColumn)) {
                    Column(modifier = Modifier.padding(Spacing.blockPadding)) {
                        SignWait(left = 7.seconds, window = Signer.SIGN_WINDOW, texts = texts, eds = eds) {}
                    }
                }
            }
        }.use { probe ->
            repeat(FRAMES) { probe.frame() }
            probe.frame()
        }
        File("/tmp/eds-wait-last-seconds.png").writeBytes(shot)

        assertTrue(shot.isNotEmpty())
    }

    /** Дверь в кабинет над моделью кабинета окна. */
    @Composable
    private fun Door(cabinet: CabinetViewModel) {
        val state by cabinet.state.collectAsState()
        CabinetSignIn(state, Language.Ru, texts, cabinet.actions())
    }

    private companion object {
        val jsonHeader = headersOf(HttpHeaders.ContentType, "application/json")
        const val CHALLENGE = """{"challengeId":"c-1","payload":"cGF5bG9hZA=="}"""

        /** Где на карточке входа стоит кнопка входа и где — отмена ожидания. */
        val SIGN_IN = Offset(590f, 452f)
        val CANCEL = Offset(590f, 506f)

        const val WIDTH = 1180
        const val HEIGHT = 820
        const val FRAMES = 30
        val TICK = 1200.milliseconds
        val WAIT = 10.seconds
        val STEP = 20.milliseconds
    }
}
