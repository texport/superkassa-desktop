package kz.mybrain.superkassa.presentation.analytics.map.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.adaptive.WrapRow
import kz.mybrain.superkassa.designsystem.format.Dates
import kz.mybrain.superkassa.designsystem.section.DetailLine
import kz.mybrain.superkassa.designsystem.state.EmptyState
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Sizes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsKkm
import kz.mybrain.superkassa.domain.analytics.model.PositionSource
import kz.mybrain.superkassa.presentation.analytics.common.KkmChips
import kz.mybrain.superkassa.presentation.analytics.common.positionWords
import kz.mybrain.superkassa.strings.api.analytics.AnalyticsTexts
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Касса, выбранная на карте.
 *
 * Стоит под картой, а не всплывает над ней: всплывающая подсказка
 * закрывает соседние точки, а сравнивать соседние кассы — ровно то,
 * ради чего владелец открыл карту.
 *
 * Пока ничего не выбрано, место карточки занято приглашением выбрать:
 * пустая рамка под картой не объясняет, зачем она там.
 *
 * @param expanded развёрнута ли карточка; свёрнутая остаётся заголовком.
 * @param onToggle сворачивание; без него стрелки у заголовка нет.
 */
@Composable
internal fun AnalyticsPinCard(
    kkm: AnalyticsKkm?,
    source: PositionSource,
    texts: AnalyticsTexts,
    cabinet: CabinetTexts,
    modifier: Modifier = Modifier,
    neighbours: Int = 0,
    onNeighbours: () -> Unit = {},
    onSales: (AnalyticsKkm) -> Unit = {},
    expanded: Boolean = true,
    onToggle: (() -> Unit)? = null
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        if (kkm == null && onToggle == null) {
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
            modifier = Modifier.fillMaxWidth().padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
        ) {
            MapCardTitle(kkm?.title ?: texts.pickPin, expanded, onToggle)
            MapCardBody(expanded) {
                if (kkm == null) {
                    PickHint(texts)
                } else {
                    KkmChips(kkm, texts, cabinet)
                    CardFacts(kkm, source, texts, cabinet)
                    CardActions(kkm, texts, neighbours, onNeighbours, onSales)
                }
            }
        }
    }
}

/**
 * Приглашение выбрать кассу — под сворачиваемым заголовком.
 *
 * Название уже стоит в заголовке, и здесь остаётся только подсказка:
 * повторять его вторым рядом незачем.
 */
@Composable
private fun PickHint(texts: AnalyticsTexts) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = AppIcons.place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(Sizes.chipIcon)
        )
        Text(
            text = texts.pickPinHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Что можно сделать с выбранной кассой.
 *
 * Главное действие одно — открыть её аналитику: карточка отвечает
 * на «что это за касса», а на «как она торгует» отвечает окно сводки.
 * Рядом с ним — возврат к соседям по месту, и только когда соседи есть:
 * у одиночной кассы возвращаться некуда. Ряд переносится: рядом с картой
 * карточка узкая, и вторая кнопка уходит под первую, а не за край.
 */
@Composable
private fun CardActions(
    kkm: AnalyticsKkm,
    texts: AnalyticsTexts,
    neighbours: Int,
    onNeighbours: () -> Unit,
    onSales: (AnalyticsKkm) -> Unit
) {
    WrapRow(modifier = Modifier.fillMaxWidth()) {
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
    DetailLine(cabinet.places.address, kkm.address)
    DetailLine(texts.lastContact, kkm.lastContactAt?.let(Dates::momentOf) ?: texts.neverSeen)
    DetailLine(texts.positionFrom, positionWords(source, texts))
    DetailLine(texts.geoSource, kkm.position?.geoSource)
}
