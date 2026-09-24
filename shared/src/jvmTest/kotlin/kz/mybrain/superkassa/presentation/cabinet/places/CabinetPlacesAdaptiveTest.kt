package kz.mybrain.superkassa.presentation.cabinet.places

import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.ProbeNode
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.presentation.cabinet.places.CabinetPlacesScene.Companion.PLACES
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.theme.TextScale
import kz.mybrain.superkassa.presentation.theme.size.ContentWidths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Раздел торговых точек в окнах от малого до широкого и на планшете.
 *
 * В окне 960×640 из двух тысяч точек было видно полторы строки: поиск,
 * отбор, счёт и две кнопки во всю ширину стояли над списком и под ним
 * неподвижно. На планшете стоймя карточка точки сжималась в столбик
 * в букву шириной, а на широком мониторе поле «Регион» тянулось на две
 * тысячи точек; колонка чтения, заведённая против этого, оставляла
 * панель наполовину пустой. Меряется семантикой: сколько строк списка видно целиком,
 * где стоят кнопки и какой ширины поля.
 */
class CabinetPlacesAdaptiveTest {

    private val names = PLACES.map { it.name }.toSet()

    /** Колонка списка — самая левая прокручиваемая часть правее рельса: рельс стоит у края окна. */
    private fun RenderProbe.tree(): ProbeNode =
        nodes().filter { it.scrolls && it.visible.height > 0 && it.at.x > 0f }.minBy { it.at.x }

    private fun RenderProbe.rows(tree: ProbeNode): List<ProbeNode> =
        nodes().filter { it.text in names && it.at.x >= tree.at.x && it.at.x < tree.at.x + tree.width }

    private fun ProbeNode.middle(): Offset = at + Offset(width / 2f, visible.height / 2f)

    @Test
    fun `в малом окне видны точки, карточка выбранной и кнопки создания`() {
        SMALL.forEach { (scene, least) ->
            scene.open("small") { probe ->
                val tree = probe.tree()
                probe.wheel(tree.middle(), WHEEL)
                val whole = probe.rows(tree).filter { it.whole }
                assertTrue(
                    whole.size >= least,
                    "${scene.width}×${scene.height} ${scene.language} ${scene.scale}: целиком видно ${whole.size} точек"
                )
                listOf(scene.texts.addRegister, scene.texts.addPlace).forEach { button ->
                    assertTrue(probe.nodes().any { it.text == button && it.whole }, "кнопка «$button» не видна целиком")
                }
                val chosen = whole.first()
                probe.click(chosen.middle())
                val card = probe.nodes().filter { it.text == chosen.text && it.at.x > tree.at.x + tree.width }
                assertTrue(card.any { it.whole }, "карточка выбранной точки не встала рядом со списком")
            }
        }
    }

    @Test
    fun `на планшете стоймя карточка сменяет список и возвращает к нему`() {
        val scene = CabinetPlacesScene(TABLET_W, TABLET_H)
        scene.open("tablet") { probe ->
            val tree = probe.tree()
            val chosen = probe.rows(tree).first { it.whole }
            probe.click(chosen.middle())
            val card = probe.nodes().filter { it.text == chosen.text && it.whole && !it.editable }
            assertEquals(1, card.size, "на узком окне список должен уступить место карточке")
            assertTrue(card.single().width > TABLET_CARD, "карточка точки сжата до ${card.single().width}")
            val back = probe.nodes().filter { it.text == scene.texts.places }.maxBy { it.at.y }
            probe.click(back.middle())
            assertTrue(probe.rows(probe.tree()).count { it.whole } > 1, "кнопка возврата не вернула список точек")
        }
    }

    @Test
    fun `на широком мониторе карточка точки занимает всю панель справа от списка`() {
        val scene = CabinetPlacesScene(WIDE_W, WIDE_H)
        scene.open("wide") { probe ->
            val tree = probe.tree()
            probe.click(probe.rows(tree).first { it.whole }.middle())
            val pane = probe.nodes().filter { it.at.x > tree.at.x + tree.width && it.width > 0 }
            val fields = pane.filter { it.editable }
            assertTrue(fields.isNotEmpty(), "у карточки точки нет полей")
            val left = pane.minOf { it.at.x }
            val right = pane.maxOf { it.at.x + it.width }
            // Прежде карточка стояла колонкой чтения, и правая часть панели пустовала.
            assertTrue(right - left > ContentWidths.reading.value, "карточка точки шириной ${right - left}")
            fields.forEach { assertTrue(it.at.x + it.width <= WIDE_W, "поле карточки за краем окна: $it") }
        }
    }

    private companion object {
        const val WHEEL = 12f
        const val TABLET_W = 800
        const val TABLET_H = 1280
        const val WIDE_W = 2560
        const val WIDE_H = 1080

        /** Уже этого карточка на планшете читалась столбиком. */
        const val TABLET_CARD = 400

        /** Малое окно и сколько точек в нём видно целиком хотя бы. */
        val SMALL = listOf(
            CabinetPlacesScene(960, 640) to 3,
            CabinetPlacesScene(960, 640, Language.Kk, TextScale.Larger) to 2
        )
    }
}
