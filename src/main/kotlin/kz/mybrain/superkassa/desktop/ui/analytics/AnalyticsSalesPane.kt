package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.history.JournalPeriodBar
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.HistoryJournalTexts
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

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
fun AnalyticsSalesPane(
    session: Session,
    cabinet: CabinetSession,
    texts: AnalyticsTexts,
    cabinetTexts: CabinetTexts
) {
    val model = remember(cabinet) { AnalyticsSalesModel(cabinet) }
    AnalyticsSales(session, model, texts, cabinetTexts, Modifier.fillMaxSize())
}

/**
 * Та же сводка, но для любого отбора.
 *
 * Отдельно от показа раздела потому, что сводка нужна и по одной кассе:
 * владелец заходит в кассу с карты и ждёт увидеть о ней то же, что видит
 * о сети. Считает её тот же [AnalyticsSalesModel] с отбором по кассе,
 * и второго экрана для неё нет.
 */
@Composable
fun AnalyticsSales(
    session: Session,
    model: AnalyticsSalesModel,
    texts: AnalyticsTexts,
    cabinetTexts: CabinetTexts,
    modifier: Modifier = Modifier
) {
    val journal = remember(session.language) { journalTexts(session.language).history }
    val enums = remember(session.language) { stringsOf(session.language).enums }
    val scope = rememberCoroutineScope()
    LaunchedEffect(model, model.period) { model.load() }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        SalesHead(model, texts, journal) { scope.launch { model.load() } }
        val view = model.view
        val state = salesState(model, texts) { scope.launch { model.load() } }
        ScreenSlot(state, Modifier.weight(1f)) {
            if (view != null) {
                AnalyticsSalesBody(view, texts, enums, journal, cabinetTexts, Modifier.weight(1f))
            }
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
private fun salesState(
    model: AnalyticsSalesModel,
    texts: AnalyticsTexts,
    onRetry: () -> Unit
): ScreenState {
    val trouble = model.trouble
    val view = model.view
    return when {
        trouble != null -> analyticsTroubleState(trouble, texts, onRetry)
        view == null -> ScreenState.Working
        view.empty -> ScreenState.Empty(AppIcons.noDocuments, texts.sales.empty, texts.sales.emptyHint)
        else -> ScreenState.Ready
    }
}

/**
 * Выбор срока и то, за какой срок посчитано.
 *
 * Даты срока полоса пишет сама — между стрелками перелистывания, — и
 * второй раз их под ней не повторяют. Исключение одно: у «всего времени»
 * границ нет, стрелок полоса не показывает вовсе, а у сводки границы
 * обязательны. Тогда они и сказаны строкой: за какой срок посчитано,
 * владелец обязан видеть всегда.
 */
@Composable
private fun SalesHead(
    model: AnalyticsSalesModel,
    texts: AnalyticsTexts,
    journal: HistoryJournalTexts,
    onRefresh: () -> Unit
) {
    JournalPeriodBar(journal, model.period, model.loading) { model.period = it }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (model.period.range == null) {
            Text(
                text = "${texts.sales.forPeriod} ${salesRangeText(model.period)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        IconButton(onClick = onRefresh, enabled = !model.loading) {
            Icon(AppIcons.refresh, contentDescription = texts.refresh)
        }
    }
}
