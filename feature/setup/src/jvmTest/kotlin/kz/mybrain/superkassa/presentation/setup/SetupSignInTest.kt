package kz.mybrain.superkassa.presentation.setup

import kz.mybrain.superkassa.CabinetStepsRig
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.domain.setup.model.SetupStep
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.presentation.common.cabinet.CabinetSteps
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Вход по ЭЦП предлагается там, где он нужен, и один раз.
 *
 * Кабинет нужен шагам кассы в кабинете и учёта в КГД. Мастер,
 * продолженный назавтра, открывается на шаге учёта с закрытым кабинетом —
 * вход должен быть прямо там, а не на пройденном шаге. Прежде, когда
 * все шаги стояли на одном экране, владелец видел две одинаковые кнопки
 * входа подряд. Кнопки ищутся по надписи.
 */
class SetupSignInTest {
    private val signIn = textsOf(Language.Ru).cabinet.signin.signIn

    @Test
    fun `на шаге кабинета вход предложен один раз, а открытому кабинету — нет`() {
        val shut = entries("cabinet-shut", SetupStep.Cabinet, CabinetStepsRig.idle(), halfway = false)
        val open = entries("cabinet-open", SetupStep.Cabinet, CabinetStepsRig.signedIn(SetupScene.NO_PLACES), false)

        assertEquals(1, shut.count { it == signIn }, "вход в кабинет предложен не один раз: $shut")
        assertTrue(open.none { it == signIn }, "в открытый кабинет предлагают войти")
        assertTrue(textsOf(Language.Ru).cabinet.enroll.add in open, "в открытом кабинете кассу завести нечем: $open")
    }

    @Test
    fun `продолженному мастеру вход предложен на шаге учёта`() {
        val shut = entries("application-shut", SetupStep.Application, CabinetStepsRig.idle(), halfway = true)

        assertEquals(1, shut.count { it == signIn }, "мастеру, продолженному назавтра, войти в кабинет нечем")
    }

    /** Надписи шага [step] поверх пройденного; кадр — в файл. */
    private fun entries(name: String, step: SetupStep, cabinet: CabinetSteps, halfway: Boolean): List<String> =
        inlineMain {
            val scene = SetupScene().started(halfway)
            val history = WizardHistory(*steps(step))
            RenderProbe(width = WIDE, height = TALL) { WizardOf(scene, history, cabinet) }.use { probe ->
                repeat(SETTLE) { probe.frame() }
                File("/tmp/audit-setup-sign-in-$name.png").writeBytes(probe.frame())
                probe.nodes().map { it.text }
            }
        }

    /** Шаги пути через кабинет до [step] включительно — как их выкладывает «Далее». */
    private fun steps(step: SetupStep): Array<String> =
        SetupStep.entries.takeWhile { it != step }.filter { it != SetupStep.Way && it != SetupStep.Credentials }
            .map { it.name }.plus(step.name).toTypedArray()

    private companion object {
        const val WIDE = 1400
        const val TALL = 900
        const val SETTLE = 40
    }
}
