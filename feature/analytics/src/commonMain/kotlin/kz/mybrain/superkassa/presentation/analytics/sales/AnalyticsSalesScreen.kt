package kz.mybrain.superkassa.presentation.analytics.sales

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.format.Dates
import kz.mybrain.superkassa.designsystem.state.ScreenSlot
import kz.mybrain.superkassa.designsystem.state.ScreenState
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.analytics.model.SalesSpan
import kz.mybrain.superkassa.domain.analytics.model.SalesView
import kz.mybrain.superkassa.presentation.analytics.common.Reading
import kz.mybrain.superkassa.presentation.analytics.common.analyticsScreenState
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.period.JournalPeriod
import kz.mybrain.superkassa.presentation.common.period.JournalPeriodBar
import kz.mybrain.superkassa.strings.api.analytics.AnalyticsTexts
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts
import kz.mybrain.superkassa.strings.api.journal.HistoryJournalTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Чем торговали кассы компании за срок.
 *
 * Считает кабинет, приложение показывает: чеки лежат у кабинета, и пять
 * чисел сверху дешевле спросить, чем выкачать ради них месяц документов.
 *
 * Срок выбирается той же полосой, что и в журнале кассы, — сегментами
 * и стрелками. Второй способ выбирать срок в одном приложении означал бы,
 * что владелец учится этому дважды.
 *
 * Ручек сводки в выложенном кабинете пока нет: до выкладки раздел
 * отвечает `404`, и экран говорит об этом словами — как и остальная
 * аналитика.
 */
@Composable
internal fun AnalyticsSalesScreen(
    model: AnalyticsSalesViewModel,
    access: String?,
    texts: AnalyticsTexts,
    cabinetTexts: CabinetTexts
) {
    LaunchedEffect(access) { model.follow(access) }
    AnalyticsSales(model, texts, cabinetTexts, Modifier.fillMaxSize())
}

/**
 * Та же сводка, но для любого отбора.
 *
 * Отдельно от показа раздела потому, что сводка нужна и по одной кассе:
 * владелец заходит в кассу с карты и ждёт увидеть о ней то же, что видит
 * о сети. Считает её та же [AnalyticsSalesViewModel] с отбором по кассе,
 * и второго экрана для неё нет.
 */
@Composable
internal fun AnalyticsSales(
    model: AnalyticsSalesViewModel,
    texts: AnalyticsTexts,
    cabinetTexts: CabinetTexts,
    modifier: Modifier = Modifier
) {
    val state by model.state.collectAsScreenState()
    val language = LocalLanguage.current
    val journal = remember(language) { textsOf(language).journal.history }
    val enums = remember(language) { textsOf(language).common.enums }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)) {
        SalesHead(state, texts, journal, model::choose, model::refresh)
        ScreenSlot(salesState(state.reading, texts, model::refresh), Modifier.weight(1f)) {
            val view = state.reading.value ?: return@ScreenSlot
            AnalyticsSalesBody(view, texts, enums, journal, cabinetTexts, Modifier.weight(1f), model.register)
        }
    }
}

/**
 * Что стоит на месте сводки.
 *
 * Пустой срок — не пустота: за ним стоит значок, строка о том, что
 * документов за этот срок нет, и подсказка взять срок шире. Ожидание
 * и отказ решаются там же, где и у остальных экранов приложения.
 */
private fun salesState(reading: Reading<SalesView>, texts: AnalyticsTexts, onRetry: () -> Unit): ScreenState =
    analyticsScreenState(reading, texts, onRetry) { view ->
        if (view.empty) ScreenState.Empty(AppIcons.noDocuments, texts.sales.empty, texts.sales.emptyHint) else null
    }

/** Срок сводки словами: всегда датами, в том числе и у «всего времени». */
internal fun salesRangeText(span: SalesSpan): String =
    "${Dates.day(span.from)} — ${Dates.day(span.to)}"

/**
 * Выбор срока и то, за какой срок посчитано.
 *
 * Даты срока полоса пишет сама — между стрелками перелистывания, — и
 * второй раз их под ней не повторяют. Исключение одно: у «всего времени»
 * границ нет, стрелок полоса не показывает вовсе, а у сводки границы
 * обязательны. Тогда они и сказаны строкой: за какой срок посчитано,
 * владелец обязан видеть всегда.
 *
 * «Обновить» стоит в ряду полосы срока, у правого края, как в адресах
 * обмена: своим рядом под полосой кнопка оставляла пустую строку.
 */
@Composable
private fun SalesHead(
    state: AnalyticsSalesUiState,
    texts: AnalyticsTexts,
    journal: HistoryJournalTexts,
    onPeriod: (JournalPeriod) -> Unit,
    onRefresh: () -> Unit
) {
    JournalPeriodBar(journal, state.period, state.reading.loading, onPeriod) {
        IconButton(onClick = onRefresh, enabled = !state.reading.loading) {
            Icon(AppIcons.refresh, contentDescription = texts.refresh)
        }
    }
    val span = state.reading.value?.range
    if (state.period.range == null && span != null) {
        Text(
            text = "${texts.sales.forPeriod} ${salesRangeText(span)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
