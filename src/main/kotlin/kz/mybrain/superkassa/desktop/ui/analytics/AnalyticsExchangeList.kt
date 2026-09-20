package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.server.cabinet.ExchangeAddress
import kz.mybrain.superkassa.desktop.ui.cabinet.cabinetMoment
import kz.mybrain.superkassa.desktop.ui.components.ScrollableList
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Адреса обмена столбцами.
 *
 * Столбцы заданной ширины, а не по содержимому: адреса и время
 * сверяют глазами сверху вниз, а колонка, гуляющая от строки к строке,
 * читается только построчно.
 *
 * Порядок строк оставлен кабинетным — по убыванию последнего обмена:
 * свежая связь важнее всего остального, и сортировать его заново здесь
 * значило бы спорить с тем, кто знает о времени больше.
 */
@Composable
fun AnalyticsExchangeList(rows: List<ExchangeAddress>, texts: AnalyticsTexts, modifier: Modifier = Modifier) {
    ScrollableList(modifier = modifier) {
        item { ExchangeHead(texts) }
        items(rows, key = { "${it.cashRegisterId}/${it.address}" }) { row ->
            ExchangeRow(row)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/** Подписи столбцов. */
@Composable
private fun ExchangeHead(texts: AnalyticsTexts) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.tight),
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        HeadCell(texts.exchangeAddress, Modifier.width(Sizes.exchangeAddressColumn))
        HeadCell(texts.kkmColumn, Modifier.weight(1f))
        HeadCell(texts.firstSeen, Modifier.width(Sizes.exchangeMomentColumn))
        HeadCell(texts.lastSeen, Modifier.width(Sizes.exchangeMomentColumn))
    }
}

/** Строка: адрес, касса и два времени. */
@Composable
private fun ExchangeRow(row: ExchangeAddress) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.tight),
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        RowCell(row.address, Modifier.width(Sizes.exchangeAddressColumn))
        RowCell(kkmTitle(row), Modifier.weight(1f))
        RowCell(cabinetMoment(row.firstSeen), Modifier.width(Sizes.exchangeMomentColumn))
        RowCell(cabinetMoment(row.lastSeen), Modifier.width(Sizes.exchangeMomentColumn))
    }
}
