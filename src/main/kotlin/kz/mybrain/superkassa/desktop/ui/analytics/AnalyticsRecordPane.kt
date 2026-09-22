package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.desktop.server.cabinet.KkmMapView
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.components.ScrollableList
import kz.mybrain.superkassa.desktop.ui.components.SectionTitle
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

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
fun AnalyticsRecordPane(cabinet: CabinetSession, texts: AnalyticsTexts) {
    val model = remember(cabinet) { AnalyticsRecordModel(cabinet) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(cabinet.token) { model.load() }
    val kkms = recordKkms(model.view)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        RecordHead(texts) { scope.launch { model.load() } }
        val state = recordState(model.view, model.trouble, texts) { scope.launch { model.load() } }
        ScreenSlot(state, Modifier.weight(1f)) {
            AnalyticsRecordBody(kkms, texts, Modifier.weight(1f))
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
internal fun recordState(
    view: KkmMapView?,
    trouble: AnalyticsTrouble?,
    texts: AnalyticsTexts,
    onRetry: () -> Unit
): ScreenState = when {
    trouble != null -> analyticsTroubleState(trouble, texts, onRetry)
    view == null -> ScreenState.Working
    recordKkms(view).isEmpty() -> ScreenState.Empty(AppIcons.kkm, texts.kkmListEmpty, texts.kkmListEmptyHint)
    else -> ScreenState.Ready
}

/** Заголовок вкладки, объяснение под значком и обновление. */
@Composable
private fun RecordHead(texts: AnalyticsTexts, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
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
fun AnalyticsRecordBody(kkms: List<AnalyticsKkm>, texts: AnalyticsTexts, modifier: Modifier = Modifier) {
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
    val record = texts.record
    ScrollableList(modifier) {
        item { RecordTiles(count, texts) }
        item { RecordSectionHead(record.refusals, record.refusalsHint) }
        if (refused.isEmpty()) {
            item { Footnote(record.refusalsNone) }
        } else {
            item { RecordRefusalsHead(texts) }
            items(refused, key = { it.cashRegisterId }) { kkm ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                RecordRefusalRow(kkm)
            }
        }
        item { RecordSectionHead(record.regions, record.regionsHint) }
        item { RecordRegionsHead(texts) }
        items(regions, key = { it.title }) { region ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            RecordRegionRow(region)
        }
    }
}

/** Заголовок раздела внутри вкладки: название и объяснение под значком. */
@Composable
private fun RecordSectionHead(title: String, hint: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = Spacing.roomy, bottom = Spacing.tight),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle(title)
        InfoTip(hint)
    }
}
