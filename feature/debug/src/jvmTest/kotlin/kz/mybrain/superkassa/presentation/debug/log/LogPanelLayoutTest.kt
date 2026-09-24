package kz.mybrain.superkassa.presentation.debug.log

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.domain.debug.model.LogEntry
import kz.mybrain.superkassa.domain.debug.model.LogLevel
import kz.mybrain.superkassa.domain.debug.model.LogSource
import kz.mybrain.superkassa.domain.debug.port.LogBookState
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Панель отбора в окне журнала переживает любую ширину окна.
 *
 * Окно журнала двигают и сужают во время разбора: в него смотрят рядом
 * с кассой, а не вместо неё. Строкой панель была `Row`, и в узком окне
 * разметка отбирала ширину у всего подряд — счётчик «Строк: 160» вставал
 * столбиком по одному знаку, а кнопки уходили за край.
 */
class LogPanelLayoutTest {

    private val texts = textsOf(Language.Ru).debug

    /** Высота того, что нарисовалось в окне заданной ширины. */
    private fun heightAt(width: Int, content: @Composable () -> Unit): Int {
        var height = 0
        RenderProbe(width = width, height = HEIGHT) {
            Box(modifier = Modifier.onGloballyPositioned { height = it.size.height }) { content() }
        }.use { it.frame() }
        return height
    }

    @Test
    fun `счётчик строк не разрывается по знакам даже в очень узком окне`() {
        val narrow = heightAt(TIGHT) { LogShownCount(texts, SHOWN) }
        println("счётчик в окне $TIGHT px: $narrow px")
        assertTrue(narrow in 1..ONE_LINE, "счётчик занял $narrow px — это больше одной строки")
    }

    @Test
    fun `панель отбора переносит ряд, а не сжимает его`() {
        val wide = heightAt(WIDE) { Panel() }
        val narrow = heightAt(NARROW) { Panel() }
        println("панель отбора: $WIDE px — $wide px, $NARROW px — $narrow px")
        assertTrue(wide > 0 && narrow > wide, "в узком окне панель не перенеслась: $wide → $narrow px")
        assertTrue(narrow <= wide * ROWS, "панель в узком окне заняла $narrow px — больше $ROWS рядов")
    }

    /** Журнал из [SHOWN] строк: счётчик над ним набран тем же числом, что и прежде. */
    private val journal = LogUiState(
        LogBookState(List(SHOWN) { LogEntry("2026-09-23 12:00:00.000", LogLevel.Info, LogSource.Machine, "GET /kkm") })
    )

    @Composable
    private fun Panel() {
        LogFilters(texts, journal, object : LogActions {})
    }

    private companion object {
        const val HEIGHT = 400
        const val WIDE = 1180
        const val NARROW = 420
        const val TIGHT = 80
        const val SHOWN = 160

        /** Сколько рядов панель вправе занять, перенесясь: сегменты, поиск, действия. */
        const val ROWS = 4

        /** Высота одной строки надписи со шкалой label: больше неё — уже перенос. */
        const val ONE_LINE = 40
    }
}
