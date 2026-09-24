package kz.mybrain.superkassa.presentation.analytics.map.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.domain.analytics.model.PlacedKkm
import kz.mybrain.superkassa.domain.analytics.model.PlacementTrouble
import kz.mybrain.superkassa.domain.analytics.model.PositionSource
import kz.mybrain.superkassa.domain.analytics.model.UnplacedKkm
import kz.mybrain.superkassa.presentation.analytics.map.sieved
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.common.list.RecordRow
import kz.mybrain.superkassa.presentation.common.list.ScrollableList
import kz.mybrain.superkassa.presentation.common.section.SectionTitle
import kz.mybrain.superkassa.presentation.common.state.EmptyState
import kz.mybrain.superkassa.presentation.common.status.StatusTone
import kz.mybrain.superkassa.presentation.common.status.toneColor
import kz.mybrain.superkassa.presentation.strings.analytics.AnalyticsTexts
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Все кассы компании списком рядом с картой.
 *
 * Стоят рядом с картой, а не исчезают из неё. Касса без адреса или
 * без координат — не отсутствующая касса, а незаконченная работа
 * владельца, и увидеть её он должен ровно там, где ищет свои кассы.
 * У каждой сказано, почему её не поставить: причины разные, и чинятся
 * они по-разному.
 *
 * @param sieved задан ли отбор. Пустой список значит разное: без отбора
 *   у владельца нет ни одной кассы, с отбором — ни одна не подошла,
 *   и одна надпись на два случая говорила бы о хозяйстве неправду.
 */
@Composable
fun AnalyticsKkmList(
    placed: List<PlacedKkm>,
    unplaced: List<UnplacedKkm>,
    chosen: String?,
    source: PositionSource,
    texts: AnalyticsTexts,
    onChoose: (PlacedKkm) -> Unit,
    modifier: Modifier = Modifier,
    sieved: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
    ) {
        SectionTitle("${texts.kkmCount} · ${Money.count(placed.size + unplaced.size)}")
        if (placed.isEmpty() && unplaced.isEmpty()) {
            EmptyState(
                icon = if (sieved) AppIcons.find else AppIcons.place,
                title = if (sieved) texts.sieve.empty else texts.kkmListEmpty,
                hint = if (sieved) texts.sieve.emptyHint else texts.kkmListEmptyHint,
                dense = true
            )
            return@Column
        }
        ScrollableList(modifier = Modifier.weight(1f)) {
            // Ключи разведены по половинам списка: одна и та же касса
            // в обеих оказаться не должна, но повторившийся ключ роняет
            // весь экран, а показанная дважды строка — нет.
            itemsIndexed(placed, key = { _, row -> "placed-${row.kkm.cashRegisterId}" }) { at, row ->
                RecordRow(
                    title = row.kkm.title,
                    subtitle = row.kkm.retailPlaceName,
                    striped = at % 2 == 1,
                    selected = chosen == row.kkm.cashRegisterId,
                    onClick = { onChoose(row) }
                )
            }
            // Чередование продолжается через обе половины: считанное
            // от нуля заново сбивало зебру на стыке, и на месте перехода
            // две строки подряд оказывались незатенёнными.
            itemsIndexed(unplaced, key = { _, row -> "unplaced-${row.kkm.cashRegisterId}" }) { at, row ->
                // Непоставленная строка не нажимается: вести карту некуда,
                // и вместо перехода у неё стоит причина — её чинят.
                RecordRow(
                    title = row.kkm.title,
                    subtitle = row.kkm.retailPlaceName,
                    striped = (placed.size + at) % 2 == 1,
                    support = { TroubleNote(row, source, texts) }
                )
            }
        }
    }
}

/** Причина, по которой кассу не поставить на карту. */
@Composable
private fun TroubleNote(row: UnplacedKkm, source: PositionSource, texts: AnalyticsTexts) {
    Text(
        text = troubleWords(row.reason, source, texts),
        style = MaterialTheme.typography.labelMedium,
        color = toneColor(troubleTone(row.reason))
    )
}

/**
 * Почему кассы нет на карте — словами владельца.
 *
 * У «положения нет вовсе» причина зависит от источника: при адресе
 * не выбран адрес, при координатах кабинета их не задал владелец,
 * при координатах кассы машина ни разу о себе не сообщила.
 */
/**
 * Роль цвета причины: поиск — ожидание, а не отказ.
 *
 * Пока адрес ищется на карте, строка красной быть не должна: красным
 * владелец читает «починить», а чинить здесь нечего — через миг касса
 * встанет на карту сама.
 */
fun troubleTone(reason: PlacementTrouble): StatusTone = when (reason) {
    PlacementTrouble.Searching -> StatusTone.Waiting
    PlacementTrouble.NotOnMap, PlacementTrouble.NoPosition -> StatusTone.Bad
}

fun troubleWords(reason: PlacementTrouble, source: PositionSource, texts: AnalyticsTexts): String = when (reason) {
    PlacementTrouble.Searching -> texts.reasonSearching
    PlacementTrouble.NotOnMap -> texts.reasonNotOnMap
    PlacementTrouble.NoPosition -> when (source) {
        PositionSource.RetailPlaceAddress -> texts.reasonNoAddress
        PositionSource.CabinetCoordinates -> texts.reasonNoCabinetPoint
        PositionSource.KkmCoordinates -> texts.reasonNoKkmPoint
    }
}
