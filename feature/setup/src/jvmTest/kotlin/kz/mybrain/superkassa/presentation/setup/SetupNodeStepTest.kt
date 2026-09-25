package kz.mybrain.superkassa.presentation.setup

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.CabinetStepsRig
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.Windowed
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.shot
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Последний шаг мастера — «касса на рабочем месте» — не выдаёт чужое за своё.
 *
 * Шаг считал себя пройденным, сверяя идентификатор БФД из пройденного
 * со списком касс. У нетронутого мастера идентификатора нет, а у кассы,
 * заведённой вручную, сведений о БФД в списке может не быть — и пустое
 * совпадало с пустым. Владелец открывал подключение и видел последний шаг
 * с галочкой и словами «Касса подключена — можно входить», не сделав
 * ни одного шага.
 */
class SetupNodeStepTest {

    @Test
    fun `чужая касса без сведений о БФД не помечает последний шаг пройденным`() {
        val alone = shot("setup-alone", SetupScene())
        val withKkm = shot("setup-with-kkm", SetupScene(kkms = listOf(CoreScene.kkm().copy(ofdSystemId = null))))

        assertTrue(
            withKkm.contentEquals(alone),
            "нетронутый мастер помечает последний шаг пройденным из-за чужой кассы"
        )
    }

    private fun shot(name: String, scene: SetupScene): ByteArray = inlineMain {
        val model = scene.model()
        val cabinet = CabinetStepsRig.signedIn(SetupScene.NO_PLACES)
        val models = SetupModels(model, scene.registration())
        val screen = @Composable { Windowed { ConnectKkmScreen(models, cabinet) {} } }
        RenderProbe(width = WIDE, height = TALL, content = screen).use { probe ->
            repeat(SETTLE) { probe.frame() }
            val frame = probe.frame()
            File("/tmp/audit-users-$name.png").writeBytes(frame)
            val done = probe.nodes().any { it.text == scene.texts.connected }
            assertTrue(!done, "$name: последний шаг назван пройденным")
            frame
        }
    }

    private companion object {
        const val WIDE = 1400
        const val TALL = 900
        const val SETTLE = 40
    }
}
