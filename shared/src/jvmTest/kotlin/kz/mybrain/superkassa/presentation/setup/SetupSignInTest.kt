package kz.mybrain.superkassa.presentation.setup

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.Look
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.Windowed
import kz.mybrain.superkassa.idleCabinet
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.mockCabinet
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.strings.cabinet.cabinetTexts
import kz.mybrain.superkassa.presentation.strings.common.Language
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Вход по ЭЦП предлагается один раз, а не двумя одинаковыми кнопками.
 *
 * Вход нужен всем шагам, кроме первого, поэтому кнопка стоит и над
 * шагами — для того, кто вернулся к мастеру на середине и у кого своей
 * кнопки не осталось ни в одном открытом шаге. Но шаг «Касса в кабинете»
 * предлагает вход и сам, и владелец, дошедший до него, видел две
 * одинаковые синие кнопки подряд: одну без единого слова над карточками,
 * вторую под объяснением, зачем она.
 *
 * Кнопки входа ищутся по надписи, а их место — по заголовку первого шага.
 */
class SetupSignInTest {
    private val signIn = cabinetTexts(Language.Ru).signIn

    @Test
    fun `над шагами не стоит второй такой же вход`() {
        val shut = look("sign-in-shut", idleCabinet())
        val open = look("sign-in-open", mockCabinet(SetupScene.NO_PLACES))

        assertEquals(0, shut.aboveSteps, "над шагами мастера стоит вторая такая же кнопка входа")
        assertEquals(1, shut.everywhere, "вход в кабинет предложен не один раз: ${shut.everywhere}")
        assertEquals(0, open.everywhere, "в открытый кабинет предлагают войти")
        assertFalse(shut.frame.contentEquals(open.frame), "закрытый и открытый кабинет на экране неразличимы")
    }

    /**
     * Продолженный назавтра мастер входом не обделён.
     *
     * Все открытые шаги такого мастера ждут кабинета и своей кнопки входа
     * не рисуют: она живёт в шаге «Касса в кабинете», а он уже пройден.
     * Без кнопки над шагами владелец упирался бы в «ждёт предыдущего шага»
     * на всех трёх оставшихся.
     */
    @Test
    fun `на середине мастера вход предлагается над шагами`() {
        val shut = look("sign-in-halfway-shut", idleCabinet(), halfway = true)
        val open = look("sign-in-halfway-open", mockCabinet(SetupScene.NO_PLACES), halfway = true)

        assertEquals(1, shut.aboveSteps, "мастеру, продолженному назавтра, войти в кабинет нечем")
        assertTrue(open.everywhere == 0, "в открытый кабинет предлагают войти")
    }

    /** Сколько кнопок входа над первым шагом и всего, и кадр — для сравнения на глаз. */
    private class Look(val aboveSteps: Int, val everywhere: Int, val frame: ByteArray)

    private fun look(name: String, cabinet: CabinetWindow, halfway: Boolean = false): Look = inlineMain {
        val scene = SetupScene().started(halfway)
        val model = scene.model()
        val models = SetupModels(model, scene.registration())
        val screen = @Composable { Windowed { ConnectKkmScreen(models, cabinet) {} } }
        RenderProbe(width = WIDE, height = TALL, content = screen).use { probe ->
            repeat(SETTLE) { probe.frame() }
            val frame = probe.frame()
            File("/tmp/audit-users-$name.png").writeBytes(frame)
            val nodes = probe.nodes()
            val firstStep = nodes.first { it.text == scene.texts.stepFactory }.at.y
            val entries = nodes.filter { it.text == signIn }
            Look(entries.count { it.at.y < firstStep }, entries.size, frame)
        }
    }

    private companion object {
        const val WIDE = 1400
        const val TALL = 900
        const val SETTLE = 40
    }
}
