package kz.mybrain.superkassa.presentation.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.Look
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.domain.analytics.model.SalesView
import kz.mybrain.superkassa.presentation.analytics.common.KkmChips
import kz.mybrain.superkassa.presentation.analytics.sales.AnalyticsSalesBody
import kz.mybrain.superkassa.presentation.analytics.sales.SalesLook
import kz.mybrain.superkassa.presentation.common.period.JournalPeriod
import kz.mybrain.superkassa.presentation.common.period.JournalPeriodBar
import kz.mybrain.superkassa.presentation.common.period.JournalSpan
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.shot
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Снимки того, что владелец увидел на живом проходе аналитики.
 *
 * Смотрит на них человек: сырой код состояния, ряд из одного столбика,
 * полоса срока в узком окне и плитки сети внутри окна одной кассы —
 * всё это разметка, и словами о ней не судят.
 */
class LiveWalkAnalyticsShots {

    private val enums = textsOf(Language.Ru).common.enums
    private val journal = textsOf(Language.Ru).journal.history

    /** Состояние смены, которого кабинет не знает: кода на экране быть не должно. */
    @Test
    fun `неизвестная смена не показывается кодом`() {
        val shot = fix("1-shift-unknown", CARD, CHIPS_HIGH) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.fieldGap),
                verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
            ) {
                KkmChips(AnalyticsLook.kkm(1), AnalyticsLook.texts, Look.cabinet)
                KkmChips(draft(), AnalyticsLook.texts, Look.cabinet)
            }
        }
        assertTrue(shot.isNotEmpty())
    }

    /** Один день в сроке: столбик, подписанное значение и дата под ним. */
    @Test
    fun `выручка за один день читается`() {
        val single = fix("2-one-day", WIDE, HIGH) { Body(SalesLook.view(days = 1, rows = 1)) }
        val week = fix("2-week", WIDE, HIGH) { Body(SalesLook.view()) }
        assertTrue(!single.contentEquals(week), "день и неделя не должны выглядеть одинаково")
    }

    /**
     * Полоса срока в окне сводки одной кассы.
     *
     * Ширина взята от самого окна: именно в нём «Сегодня» рассыпалось
     * на отдельные буквы по вертикали. Вторым снимком — вдвое уже: полоса
     * обязана читаться на любой ширине, а не только на этой.
     */
    @Test
    fun `полоса срока переносится в узком окне`() {
        val dialog = fix("3-period-bar-dialog", DIALOG, BAR_HIGH) { PeriodBar() }
        val narrow = fix("3-period-bar-narrow", NARROW, BAR_HIGH) { PeriodBar() }
        assertTrue(!dialog.contentEquals(narrow), "полоса обязана перестраиваться по ширине")
    }

    @Composable
    private fun PeriodBar() {
        Column(modifier = Modifier.fillMaxWidth().padding(Spacing.cardPadding)) {
            JournalPeriodBar(journal, JournalPeriod.of(JournalSpan.Week).shiftedBy(-1), false) {}
        }
    }

    /** Окно одной кассы: плитки числа касс в нём нет, сводка сети не тронута. */
    @Test
    fun `в окне одной кассы нет плитки числа касс`() {
        val one = fix("4-one-kkm", WIDE, HIGH) { Body(SalesLook.view(), register = "c-1") }
        val all = fix("4-network", WIDE, HIGH) { Body(SalesLook.view()) }
        assertTrue(!one.contentEquals(all), "окно кассы и сводка сети обязаны различаться плиткой")
    }

    @Composable
    private fun Body(view: SalesView, register: String? = null) {
        AnalyticsSalesBody(
            view = view,
            texts = AnalyticsLook.texts,
            enums = enums,
            journal = journal,
            cabinet = Look.cabinet,
            modifier = Modifier.fillMaxSize(),
            register = register
        )
    }

    /** Касса-черновик: кабинет отдаёт по ней `UNKNOWN` вместо состояния смены. */
    private fun draft() = AnalyticsLook.kkm(2).copy(status = "DRAFT", shiftStatus = "UNKNOWN", shiftNumber = null)

    private companion object {
        const val WIDE = 1180
        const val HIGH = 820
        const val CARD = 720
        const val CHIPS_HIGH = 160

        /** Ширина окна сводки одной кассы: в нём полоса срока и не влезла. */
        const val DIALOG = 900
        const val NARROW = 520
        const val BAR_HIGH = 160
        const val SETTLE = 40
    }

    /** Снимок под тем же именем, что и пункт списка владельца. */
    private fun fix(name: String, width: Int, height: Int, content: @Composable () -> Unit): ByteArray =
        RenderProbe(width = width, height = height, content = content).use { probe ->
            var frame = probe.frame()
            repeat(SETTLE) { frame = probe.frame() }
            File("/tmp/fix-$name.png").writeBytes(frame)
            frame
        }
}
