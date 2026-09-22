package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.desktop.server.cabinet.KkmMapView
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsRecordBody
import kz.mybrain.superkassa.desktop.ui.analytics.recordState
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.analyticsTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Вкладка учёта на сцене без окна.
 *
 * Здесь смотрят на то, чего числами не проверить: читается ли парк
 * с одного взгляда, находится ли столбец отказов глазом и не разъезжается
 * ли таблица областей. Ими же меряется и цена отрисовки: сеть показа —
 * это три тысячи касс, и вкладка обязана собираться за доли секунды
 * при любой их длине.
 */
class AnalyticsRecordShots {

    private val texts = analyticsTexts(Language.Ru)

    @Composable
    private fun Body(kkms: List<AnalyticsKkm>) {
        Box(modifier = Modifier.fillMaxSize()) {
            AnalyticsRecordBody(kkms, texts, Modifier.fillMaxSize())
        }
    }

    /** Сеть показа: три тысячи касс, из них на учёте четыре. */
    @Test
    fun `сеть показа`() = shot("an-record-show", RecordFleet.show())

    /** Смешанный парк: отказы КГД и блокировки на виду. */
    @Test
    fun `отказы и блокировки`() = shot("an-record-mixed", RecordFleet.mixed())

    /** Отказов больше, чем помещается в окно: список уходит под край. */
    @Test
    fun `много отказов`() = shot("an-record-refusals", RecordFleet.refusals(MANY))

    /** Кабинет без единой кассы: значок, строка о пустоте и что сделать сейчас. */
    @Test
    fun `пустой кабинет`() {
        val state = recordState(KkmMapView(), trouble = null, texts = texts) {}

        assertEquals(ScreenState.Empty(AppIcons.kkm, texts.kkmListEmpty, texts.kkmListEmptyHint), state)
        RenderProbe { ScreenSlot(state, Modifier.fillMaxSize()) {} }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            Look.shot("an-record-empty", probe.frame())
        }
    }

    /**
     * Отрисовка вкладки не зависит от длины парка.
     *
     * Три тысячи касс складываются в восемь чисел и два десятка строк,
     * и собранный целиком список отказов был бы единственным местом,
     * где длина парка доходила бы до разметки. Проверяется, что не доходит.
     */
    @Test
    fun `вкладка рисуется быстро и при трёх тысячах касс`() {
        renderMillis { Body(RecordFleet.show(count = SMALL)) }
        val small = renderMillis { Body(RecordFleet.show(count = SMALL)) }
        val large = renderMillis { Body(RecordFleet.show(count = LARGE)) }
        println("учёт касс: $SMALL касс — $small мс, $LARGE касс — $large мс")
        assertTrue(large < BUDGET, "$LARGE касс рисуются $large мс")
        assertTrue(large < small * FACTOR + SLACK, "рост отрисовки с длиной парка: $small → $large мс")
    }

    /** Список отказов длиннее окна: вкладка прокручивается до областей. */
    @Test
    fun `вкладка прокручивается колесом мыши`() {
        RenderProbe { Body(RecordFleet.refusals(MANY)) }.use { probe ->
            val before = probe.frame()
            probe.wheel(at = Offset(400f, 400f), ticks = 6f)
            assertTrue(probe.changedFrom(before), "картинка вкладки не изменилась после прокрутки")
        }
    }

    private fun shot(name: String, kkms: List<AnalyticsKkm>) {
        RenderProbe { Body(kkms) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            Look.shot(name, probe.frame())
        }
    }

    private companion object {
        /** Короткий парк: с ним сравнивается цена отрисовки длинного. */
        const val SMALL = 5

        /** Сеть показа с запасом: столько касс увидит гость. */
        const val LARGE = 3300

        /** Сколько миллисекунд отводится на сборку и отрисовку вкладки. */
        const val BUDGET = 1500L

        /** Во сколько раз длинный парк вправе оказаться дороже короткого. */
        const val FACTOR = 2

        /** Запас на разогрев машины, не зависящий от длины парка. */
        const val SLACK = 150L

        /** Столько отказов не помещается в окно: на них и проверяется прокрутка. */
        const val MANY = 60

        /** Сколько кадров даётся сцене, чтобы встать до снимка. */
        const val SETTLE = 12
    }
}
