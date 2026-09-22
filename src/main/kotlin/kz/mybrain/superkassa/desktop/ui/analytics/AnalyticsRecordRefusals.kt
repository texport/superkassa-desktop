package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.desktop.ui.cabinet.cabinetMoment
import kz.mybrain.superkassa.desktop.ui.components.StatusTone
import kz.mybrain.superkassa.desktop.ui.components.toneColor
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Кассы, которым КГД отказал в учёте.
 *
 * Единственное на вкладке, по чему владелец действует руками: отказ
 * сам не пройдёт, заявление нужно разобрать и подать заново. Потому
 * отказы стоят выше разреза по регионам — до них доходят глазами
 * раньше, чем до областей.
 *
 * В строке то, чем касса опознаётся в заявлении и на месте: своё
 * название, номер КГД, торговая точка с адресом и время последней связи.
 * Причины отказа в ответе кабинета нет — её читают в самом заявлении,
 * и выдумывать её здесь нельзя.
 */
@Composable
internal fun RecordRefusalsHead(texts: AnalyticsTexts) {
    TableRow {
        HeadCell(texts.kkmColumn, Modifier.width(Sizes.salesNameColumn))
        HeadCell(texts.registrationNumber, Modifier.width(Sizes.salesNumberColumn))
        HeadCell(texts.retailPlace, Modifier.weight(1f))
        HeadCell(texts.lastContact, Modifier.width(Sizes.exchangeMomentColumn))
    }
}

/** Строка отказа: касса, её номер, где она стоит и когда выходила на связь. */
@Composable
internal fun RecordRefusalRow(kkm: AnalyticsKkm) {
    TableRow(Modifier.padding(vertical = Spacing.tight)) {
        Text(
            text = kkmTitle(kkm),
            style = MaterialTheme.typography.bodyMedium,
            color = toneColor(StatusTone.Bad),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(Sizes.salesNameColumn)
        )
        RowCell(kkm.registrationNumber ?: Glyphs.DASH, Modifier.width(Sizes.salesNumberColumn))
        RowCell(placeWords(kkm), Modifier.weight(1f))
        RowCell(cabinetMoment(kkm.lastContactAt), Modifier.width(Sizes.exchangeMomentColumn))
    }
}

/**
 * Где стоит эта касса — точкой и адресом в одной клетке.
 *
 * Врозь они заняли бы два столбца из четырёх, а читают их вместе:
 * «Магазин на Абая» без адреса не отличить от такого же в соседнем
 * городе, а адрес без названия точки владелец не узнаёт.
 */
private fun placeWords(kkm: AnalyticsKkm): String = listOfNotNull(
    kkm.retailPlaceName?.takeIf(String::isNotBlank),
    kkm.address?.takeIf(String::isNotBlank)
).joinToString(Glyphs.SEPARATOR).ifBlank { Glyphs.DASH }
