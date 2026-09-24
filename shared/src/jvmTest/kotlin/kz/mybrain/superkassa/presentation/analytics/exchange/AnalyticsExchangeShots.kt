package kz.mybrain.superkassa.presentation.analytics.exchange

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.Look
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsTrouble
import kz.mybrain.superkassa.domain.analytics.model.ExchangeAddress
import kz.mybrain.superkassa.presentation.analytics.AnalyticsLook
import kz.mybrain.superkassa.presentation.analytics.common.analyticsTroubleState
import kz.mybrain.superkassa.presentation.common.state.ScreenSlot
import kz.mybrain.superkassa.presentation.common.state.ScreenState
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kotlin.test.Test

/**
 * Снимки адресов обмена.
 *
 * Сведение служебное, и его отказные состояния путать нельзя: пусто,
 * потому что касса ни разу не выходила на связь, — это не то же, что
 * пусто, потому что поиск ничего не нашёл.
 */
class AnalyticsExchangeShots {

    /** Сотня адресов: столбцы не должны разъехаться от строки к строке. */
    @Test
    fun `сотня адресов обмена`() {
        val rows = (1..HUNDRED).map(::address)
        RenderProbe(WIDE, HIGH) { AnalyticsExchangeList(rows, AnalyticsLook.texts, Modifier.fillMaxSize()) }
            .use { probe ->
                repeat(SETTLE) { probe.frame() }
                Look.shot("an-exchange-hundred", probe.frame())
            }
    }

    /** Ни одного обмена: касса ещё ни разу не выходила на связь. */
    @Test
    fun `ни одного обмена`() = state(
        "an-exchange-empty",
        ScreenState.Empty(
            AppIcons.noDocuments,
            AnalyticsLook.texts.exchangeEmpty,
            AnalyticsLook.texts.exchangeEmptyHint
        )
    )

    /** Поиск ничего не нашёл: обмены есть, но не такие. */
    @Test
    fun `поиск ничего не нашёл`() = state(
        "an-exchange-not-found",
        ScreenState.Empty(AppIcons.find, AnalyticsLook.texts.exchangeNotFound, AnalyticsLook.texts.exchangeNotFoundHint)
    )

    /** Кабинет не ответил. */
    @Test
    fun `кабинет не ответил`() =
        state("an-exchange-unreachable", analyticsTroubleState(AnalyticsTrouble.Unreachable, AnalyticsLook.texts) {})

    /** Раздел ещё не выложен. */
    @Test
    fun `раздел не выложен`() =
        state("an-exchange-not-deployed", analyticsTroubleState(AnalyticsTrouble.NotDeployed, AnalyticsLook.texts) {})

    private fun state(name: String, screen: ScreenState) {
        RenderProbe(WIDE, HIGH) { ScreenSlot(screen, Modifier.fillMaxSize()) {} }
            .use { probe ->
                repeat(SETTLE) { probe.frame() }
                Look.shot(name, probe.frame())
            }
    }

    /** Адрес обмена: часть касс выходит с одного, часть ни разу не выходила. */
    private fun address(at: Int) = ExchangeAddress(
        cashRegisterId = "c$at",
        kkmId = 2000300 + at,
        registrationNumber = "%012d".format(4500000L + at),
        internalName = "Касса $at",
        retailPlaceName = "Магазин $at",
        address = "212.154.%d.%d".format(at / DOTS, at % DOTS),
        firstSeen = "2026-09-01T06:00:00Z",
        lastSeen = if (at % DOTS == 0) null else "2026-09-20T19:47:00Z"
    )

    private companion object {
        const val HUNDRED = 100
        const val DOTS = 10
        const val SETTLE = 24
        const val WIDE = 1180
        const val HIGH = 820
    }
}
