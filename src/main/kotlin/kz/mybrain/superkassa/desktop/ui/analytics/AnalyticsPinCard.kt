package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.desktop.server.cabinet.PositionSource
import kz.mybrain.superkassa.desktop.ui.cabinet.CabinetStatusChip
import kz.mybrain.superkassa.desktop.ui.cabinet.cabinetMoment
import kz.mybrain.superkassa.desktop.ui.cabinet.statusTitle
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.DetailLine
import kz.mybrain.superkassa.desktop.ui.components.EmptyState
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Касса, выбранная на карте.
 *
 * Стоит под картой, а не всплывает над ней: всплывающая подсказка
 * закрывает соседние точки, а сравнивать соседние кассы — ровно то,
 * ради чего владелец открыл карту.
 *
 * Пока ничего не выбрано, место карточки занято приглашением выбрать:
 * пустая рамка под картой не объясняет, зачем она там.
 */
@Composable
fun AnalyticsPinCard(
    kkm: AnalyticsKkm?,
    source: PositionSource,
    texts: AnalyticsTexts,
    cabinet: CabinetTexts,
    modifier: Modifier = Modifier,
    neighbours: Int = 0,
    onNeighbours: () -> Unit = {},
    onSales: (AnalyticsKkm) -> Unit = {}
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        if (kkm == null) {
            EmptyState(
                icon = AppIcons.place,
                title = texts.pickPin,
                hint = texts.pickPinHint,
                dense = true,
                centered = true
            )
            return@OutlinedCard
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.normal),
            verticalArrangement = Arrangement.spacedBy(Spacing.tight)
        ) {
            CardHead(kkm, texts, cabinet)
            CardFacts(kkm, source, texts, cabinet)
            CardActions(kkm, texts, neighbours, onNeighbours, onSales)
        }
    }
}

/** Название кассы и её состояние: то, что читают первым. */
@Composable
private fun CardHead(kkm: AnalyticsKkm, texts: AnalyticsTexts, cabinet: CabinetTexts) {
    Text(
        text = kkmTitle(kkm),
        style = MaterialTheme.typography.titleMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(Spacing.tight)) {
        CabinetStatusChip(kkm.status, cabinet)
        if (kkm.blocked) Chip(text = texts.blocked, color = StatusColors.refused)
        kkm.shiftStatus?.takeIf { it.isNotBlank() }?.let { shift ->
            Chip(text = shiftWords(shift, kkm.shiftNumber, cabinet), color = StatusColors.pending)
        }
    }
}

/**
 * Что можно сделать с выбранной кассой.
 *
 * Главное действие одно — открыть её аналитику: карточка отвечает
 * на «что это за касса», а на «как она торгует» отвечает окно сводки.
 * Рядом с ним — возврат к соседям по месту, и только когда соседи есть:
 * у одиночной кассы возвращаться некуда.
 */
@Composable
private fun CardActions(
    kkm: AnalyticsKkm,
    texts: AnalyticsTexts,
    neighbours: Int,
    onNeighbours: () -> Unit,
    onSales: (AnalyticsKkm) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = { onSales(kkm) }) { Text(texts.openKkmSales) }
        if (neighbours > 1) {
            TextButton(onClick = onNeighbours) { Text("${texts.kkmsHere} · $neighbours") }
        }
    }
}

/** Реквизиты кассы строками «подпись — значение». */
@Composable
private fun CardFacts(kkm: AnalyticsKkm, source: PositionSource, texts: AnalyticsTexts, cabinet: CabinetTexts) {
    DetailLine(texts.registrationNumber, kkm.registrationNumber ?: texts.noRegistrationNumber)
    DetailLine(texts.retailPlace, kkm.retailPlaceName)
    DetailLine(cabinet.placeAddress, kkm.address)
    DetailLine(texts.lastContact, kkm.lastContactAt?.let(::cabinetMoment) ?: texts.neverSeen)
    DetailLine(texts.positionFrom, positionWords(source, texts))
    DetailLine(texts.geoSource, kkm.position?.geoSource)
}

/** Смена: её состояние и номер одной плашкой. */
private fun shiftWords(status: String, number: Long?, cabinet: CabinetTexts): String =
    listOfNotNull(statusTitle(status, cabinet), number?.let { "№ $it" }).joinToString(" · ")
