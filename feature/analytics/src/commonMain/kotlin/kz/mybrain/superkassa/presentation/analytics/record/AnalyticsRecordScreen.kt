package kz.mybrain.superkassa.presentation.analytics.record

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.list.ScrollableList
import kz.mybrain.superkassa.designsystem.section.SectionTitle
import kz.mybrain.superkassa.designsystem.state.ScreenSlot
import kz.mybrain.superkassa.designsystem.state.ScreenState
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.tip.InfoTip
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsKkm
import kz.mybrain.superkassa.domain.analytics.model.KkmMapView
import kz.mybrain.superkassa.domain.analytics.model.RecordCount
import kz.mybrain.superkassa.domain.analytics.model.RecordRegion
import kz.mybrain.superkassa.domain.analytics.model.recordCount
import kz.mybrain.superkassa.domain.analytics.model.recordRegions
import kz.mybrain.superkassa.domain.analytics.model.refusedKkms
import kz.mybrain.superkassa.presentation.analytics.common.AcrossBar
import kz.mybrain.superkassa.presentation.analytics.common.Reading
import kz.mybrain.superkassa.presentation.analytics.common.TableAcross
import kz.mybrain.superkassa.presentation.analytics.common.analyticsScreenState
import kz.mybrain.superkassa.presentation.analytics.common.rememberTableAcross
import kz.mybrain.superkassa.presentation.analytics.sales.chart.Footnote
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.strings.api.analytics.AnalyticsTexts

/**
 * Как ведётся парк касс компании.
 *
 * Кассу заводят в кабинете, а на учёт её ставит КГД по заявлению, и эти
 * два события разнесены на недели: в сети показа из трёх тысяч касс
 * на учёте четыре. Вкладка отвечает на вопрос об этом разрыве — сколько
 * касс заведено, сколько учтено, по скольким КГД отказал и как это
 * разложено по областям страны.
 *
 * Кабинет спрашивается той же ручкой, что и карта: всё, из чего
 * складывается учёт, уже лежит в её ответе.
 */
@Composable
internal fun AnalyticsRecordScreen(model: AnalyticsRecordViewModel, access: String?, texts: AnalyticsTexts) {
    val reading by model.state.collectAsScreenState()
    LaunchedEffect(access) { model.follow(access) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
    ) {
        RecordHead(texts, model::refresh)
        ScreenSlot(recordState(reading, texts, model::refresh), Modifier.weight(1f)) {
            AnalyticsRecordBody(reading.value?.kkms.orEmpty(), texts, Modifier.weight(1f))
        }
    }
}

/**
 * Что стоит на месте вкладки.
 *
 * Пустой кабинет — не пустота: за ним значок кассы, строка о том, что
 * касс нет ни одной, и что сделать сейчас — завести кассу и подать
 * заявление. Слова те же, что и у пустого списка карты: положение
 * владельца одно и то же, и рассказывать о нём двумя разными способами
 * незачем. Ожидание и отказ решаются там же, где и у остальных экранов.
 */
internal fun recordState(reading: Reading<KkmMapView>, texts: AnalyticsTexts, onRetry: () -> Unit): ScreenState =
    analyticsScreenState(reading, texts, onRetry) { view ->
        if (view.kkms.isEmpty()) ScreenState.Empty(AppIcons.kkm, texts.kkmListEmpty, texts.kkmListEmptyHint) else null
    }

/** Заголовок вкладки, объяснение под значком и обновление. */
@Composable
private fun RecordHead(texts: AnalyticsTexts, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle(texts.record.title)
        InfoTip(texts.record.hint)
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onRefresh) {
            Icon(AppIcons.refresh, contentDescription = texts.refresh)
        }
    }
}

/**
 * Сама вкладка: числа парка, отказы и разрез по областям.
 *
 * Разрезы считаются от набора касс, а не на каждом кадре: у сети в три
 * тысячи машин пересчёт по четырём смыслам и двум десяткам областей
 * повторялся бы при всяком движении полосы прокрутки.
 *
 * Вкладка — один ленивый список, а не столбец с таблицами внутри:
 * отказов у сети бывают сотни, и собранные целиком они стоили бы
 * своей высоты на каждом кадре. Из этого же следует, что разделы
 * здесь разделены заголовками, а не карточками: карточка вокруг
 * ленивого списка требует от него полной высоты, и ленивым он быть
 * перестаёт.
 */
@Composable
internal fun AnalyticsRecordBody(kkms: List<AnalyticsKkm>, texts: AnalyticsTexts, modifier: Modifier = Modifier) {
    val unknown = texts.sales.noAddress
    val count = remember(kkms) { recordCount(kkms) }
    val regions = remember(kkms, unknown) { recordRegions(kkms, unknown) }
    val refused = remember(kkms) { refusedKkms(kkms) }
    RecordSections(count, refused, regions, texts, modifier)
}

/** Разделы вкладки по порядку: числа парка, отказы КГД, области. */
@Composable
private fun RecordSections(
    count: RecordCount,
    refused: List<AnalyticsKkm>,
    regions: List<RecordRegion>,
    texts: AnalyticsTexts,
    modifier: Modifier
) {
    // Таблицы меряют место под собой сами: строки живут в общем списке
    // вкладки, и ширину им назначает он, за вычетом поля под полосу.
    BoxWithConstraints(modifier = modifier) {
        val room = maxWidth - Spacing.scrollbarGutter
        val refusals = rememberTableAcross(REFUSAL_COLUMNS, room)
        val areas = rememberTableAcross(REGION_COLUMNS, room)
        ScrollableList(Modifier.fillMaxSize()) {
            item { RecordTiles(count, texts) }
            refusalItems(refused, refusals, texts)
            regionItems(regions, areas, texts)
        }
    }
}

/** Отказы КГД: заголовок раздела и таблица, а без отказов — строка об этом. */
private fun LazyListScope.refusalItems(refused: List<AnalyticsKkm>, table: TableAcross, texts: AnalyticsTexts) {
    item { RecordSectionHead(texts.record.refusals, texts.record.refusalsHint) }
    if (refused.isEmpty()) {
        item { Footnote(texts.record.refusalsNone) }
        return
    }
    item { RecordRefusalsHead(table, texts) }
    items(refused, key = { it.cashRegisterId }) { kkm ->
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        RecordRefusalRow(table, kkm)
    }
    item { AcrossBar(table) }
}

/** Разрез по областям: заголовок раздела и таблица. */
private fun LazyListScope.regionItems(regions: List<RecordRegion>, table: TableAcross, texts: AnalyticsTexts) {
    item { RecordSectionHead(texts.record.regions, texts.record.regionsHint) }
    item { RecordRegionsHead(table, texts) }
    items(regions, key = { it.title }) { region ->
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        RecordRegionRow(table, region)
    }
    item { AcrossBar(table) }
}

/** Заголовок раздела внутри вкладки: название и объяснение под значком. */
@Composable
private fun RecordSectionHead(title: String, hint: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.blockPadding, bottom = Spacing.itemGap),
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle(title)
        InfoTip(hint)
    }
}
