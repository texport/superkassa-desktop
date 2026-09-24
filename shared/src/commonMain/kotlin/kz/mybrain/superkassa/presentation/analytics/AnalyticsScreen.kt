package kz.mybrain.superkassa.presentation.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.presentation.analytics.exchange.AnalyticsExchangeScreen
import kz.mybrain.superkassa.presentation.analytics.exchange.analyticsExchangeViewModel
import kz.mybrain.superkassa.presentation.analytics.map.AnalyticsMapScreen
import kz.mybrain.superkassa.presentation.analytics.map.MapWords
import kz.mybrain.superkassa.presentation.analytics.map.analyticsMapViewModel
import kz.mybrain.superkassa.presentation.analytics.map.mapTools
import kz.mybrain.superkassa.presentation.analytics.record.AnalyticsRecordScreen
import kz.mybrain.superkassa.presentation.analytics.record.analyticsRecordViewModel
import kz.mybrain.superkassa.presentation.analytics.sales.AnalyticsSalesScreen
import kz.mybrain.superkassa.presentation.analytics.sales.analyticsSalesViewModel
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.strings.analytics.AnalyticsTexts
import kz.mybrain.superkassa.presentation.strings.analytics.analyticsTexts
import kz.mybrain.superkassa.presentation.strings.cabinet.CabinetTexts
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Аналитика по кассам компании.
 *
 * Четыре взгляда на одно хозяйство: где кассы стоят, как ведётся их
 * учёт, откуда они выходят на связь и чем торгуют. Разведены вкладками,
 * потому что смотрят на них порознь — карту открывают, чтобы найти
 * кассу глазами, адреса обмена, чтобы разобраться, почему касса
 * отвечает не оттуда, где числится, торговлю — чтобы увидеть выручку
 * за срок, а учёт — чтобы увидеть, какая часть парка вправе торговать.
 * Вместе на одном экране они боролись бы за высоту: карте нужна вся,
 * списку адресов и сводке — тоже.
 *
 * Вкладки здесь второго уровня: первого уровня заняты разделами
 * кабинета, и одинаковые полосы вкладок одна под другой не читались бы
 * как разные уровни.
 *
 * @param access отметка вошедшего в кабинет; `null` — владелец не входил,
 *   и взглядам показывать нечего.
 */
@Composable
fun AnalyticsScreen(app: AppContainer, access: String?, cabinetTexts: CabinetTexts) {
    val language = LocalLanguage.current
    val texts = remember(language) { analyticsTexts(language) }
    var page by remember { mutableStateOf(AnalyticsTab.Map) }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
    ) {
        AnalyticsTabs(page, texts) { page = it }
        when (page) {
            AnalyticsTab.Map -> {
                val words = MapWords(texts, cabinetTexts)
                AnalyticsMapScreen(app, analyticsMapViewModel(app), mapTools(app), access, words)
            }
            AnalyticsTab.Record -> AnalyticsRecordScreen(analyticsRecordViewModel(app), access, texts)
            AnalyticsTab.Exchange -> AnalyticsExchangeScreen(analyticsExchangeViewModel(app), access, texts)
            AnalyticsTab.Sales -> AnalyticsSalesScreen(analyticsSalesViewModel(app), access, texts, cabinetTexts)
        }
    }
}

/** Переключение взглядов. */
@Composable
private fun AnalyticsTabs(page: AnalyticsTab, texts: AnalyticsTexts, onSelect: (AnalyticsTab) -> Unit) {
    SecondaryTabRow(selectedTabIndex = page.ordinal, containerColor = Color.Transparent) {
        AnalyticsTab.entries.forEach { tab ->
            Tab(
                selected = tab == page,
                onClick = { onSelect(tab) },
                text = { Text(text = tab.title(texts), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
    }
}

/** Взгляды аналитики. */
/**
 * Взгляды аналитики по порядку, в каком их открывают.
 *
 * Учёт стоит сразу за картой: карту открывают первой и спрашивают
 * у неё, где кассы, а следующий вопрос о них — вправе ли они торговать.
 * Прежде учёт стоял последним, за торговлей, до которой у сети,
 * не вставшей на учёт, дело ещё не дошло.
 */
enum class AnalyticsTab(val title: (AnalyticsTexts) -> String) {
    Map({ it.mapTab }),
    Record({ it.record.tab }),
    Exchange({ it.exchangeTab }),
    Sales({ it.sales.tab })
}
