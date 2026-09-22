package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.desktop.server.cabinet.RetailPlace
import kz.mybrain.superkassa.desktop.ui.cabinet.ActionKind
import kz.mybrain.superkassa.desktop.ui.cabinet.ApplicationFields
import kz.mybrain.superkassa.desktop.ui.cabinet.DeregistrationReason
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.io.File
import kotlin.test.Test

/**
 * Снимки раздела точек у сети: две тысячи точек и столько же касс.
 *
 * Столько их будет у владельца, ради которого кабинет и делается.
 * [PlaceShots] снимает хозяйство в несколько точек — там видно каждое
 * состояние точки, — а здесь проверяется другое: длинный список, поиск
 * по нему, выбор точки из двух тысяч и длинные названия в узком окне.
 * Что список при этом рисуется быстро, проверяет [BigListRenderTest].
 */
class PlaceScaleShots {

    private val texts = cabinetTexts(Language.Ru)

    private fun places(count: Int, long: Boolean = false) = (1..count).map {
        RetailPlace(
            id = "p$it",
            name = if (long) "Магазин «Сауда орталығы Достык Плаза» отдел $it" else "Магазин на Абая $it",
            address = if (long) {
                "Республика Казахстан, г. Алматы, Медеуский район, проспект Достык, дом 111, помещение $it"
            } else {
                "г. Алматы, пр. Абая, $it"
            },
            cashRegisterCount = 1
        )
    }

    private fun registers(places: List<RetailPlace>) = places.map {
        PlaceLook.register(it.id.drop(1).toInt(), it.id)
    }

    @Composable
    private fun Picker(count: Int) {
        Column(modifier = Modifier.fillMaxSize().padding(Spacing.screen)) {
            ApplicationFields(
                kind = ActionKind.Reregistration,
                texts = texts,
                language = Language.Ru,
                places = places(count),
                placeId = "",
                reason = DeregistrationReason.CessationOfUse,
                comment = "",
                onPlace = {},
                onReason = {},
                onComment = {}
            )
        }
    }

    @Test
    fun `две тысячи точек в окне`() {
        val all = places(TWO_THOUSAND)
        shot("scale-places", WIDE, TALL) { PlacesLook(all, registers(all), open = "p3") }
        shot("scale-places-narrow", NARROW, SHORT) { PlacesLook(all, registers(all), open = "p3") }
        shot("scale-places-found", WIDE, TALL) {
            PlacesLook(all, registers(all), open = "p3", query = "Абая 1999")
        }
        val long = places(TWO_THOUSAND, long = true)
        shot("scale-places-long", WIDE, TALL) { PlacesLook(long, registers(long), open = "p3") }
        shot("scale-places-long-narrow", NARROW, SHORT) { PlacesLook(long, registers(long), open = "p3") }
    }

    @Test
    fun `выбор точки среди двух тысяч`() {
        RenderProbe(width = WIDE, height = TALL) { Picker(TWO_THOUSAND) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.click(Offset(FIELD_X, FIELD_Y))
            File("/tmp/cabinet-scale-picker-open.png").writeBytes(probe.frame())
            probe.type("Достык 1999")
            repeat(SETTLE) { probe.frame() }
            File("/tmp/cabinet-scale-picker-typed.png").writeBytes(probe.frame())
        }
    }

    private companion object {
        const val TWO_THOUSAND = 2000
        const val WIDE = 1372
        const val TALL = 887
        const val NARROW = 1000
        const val SHORT = 700
        const val SETTLE = 20
        const val FIELD_X = 300f
        const val FIELD_Y = 40f
    }
}
