package kz.mybrain.superkassa.presentation.sale

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.presentation.adaptive.MoneyText
import kz.mybrain.superkassa.presentation.adaptive.WrapRow
import kz.mybrain.superkassa.presentation.components.Money
import kz.mybrain.superkassa.presentation.strings.LocalStrings
import kz.mybrain.superkassa.presentation.theme.AppIcons
import kz.mybrain.superkassa.presentation.theme.KassaLayout
import kz.mybrain.superkassa.presentation.theme.TableColumns

/**
 * Строка корзины.
 *
 * Наименование идёт первой строкой во всю ширину листа, под ним — из чего
 * сложилась строка, её сумма и действия. Прежде сумма и три значка стояли
 * справа от наименования постоянной шириной, и в окне по умолчанию имя
 * товара сжималось до одной буквы. Когда листу тесно, сумма с действиями
 * переносится под состав строки, а не отнимает место у наименования.
 *
 * Строка списка, а не карточка: рамка вокруг позиции ставила карточку
 * в карточку листа чека.
 *
 * Сторно окрашено ролью ошибки целиком, а не помечено одним словом: строка,
 * уводящая итог вниз, обязана отличаться от продажи с одного взгляда.
 *
 * Нажатие на саму строку открывает её подробности; значки перехватывают
 * своё нажатие и до строки его не доносят, поэтому сторно и удаление
 * работают как прежде.
 */
@Composable
internal fun PositionRow(
    position: Position,
    onOpen: () -> Unit,
    onStorno: () -> Unit,
    onExcise: () -> Unit,
    onRemove: () -> Unit
) {
    val texts = LocalStrings.current
    ListItem(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = positionColors(position.storno),
        // Наименование не растёт больше чем на две строки: у товара
        // с каталожным именем в шесть слов строка вырастала до шести строк,
        // и в лист чека помещалось четыре позиции. Целиком имя, количество,
        // цену и ставку показывает окно подробностей.
        headlineContent = {
            Text(
                text = position.label(texts.sale.storno),
                style = MaterialTheme.typography.titleMedium,
                maxLines = NAME_LINES,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = { PositionFigures(position, onStorno, onExcise, onRemove) }
    )
}

/**
 * Цвета строки: обычная лежит на подложке листа, сторно берёт роли ошибки.
 *
 * Своей поверхности у обычной строки нет: белая строка на тональном листе
 * читалась бы карточкой в карточке.
 */
@Composable
private fun positionColors(storno: Boolean) = if (storno) {
    ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        headlineColor = MaterialTheme.colorScheme.onErrorContainer,
        supportingColor = MaterialTheme.colorScheme.onErrorContainer
    )
} else {
    ListItemDefaults.colors(containerColor = Color.Transparent)
}

/**
 * Состав строки, её сумма и действия.
 *
 * Состав тянется на остаток ряда; не помещается он рядом с суммой —
 * сумма и действия переносятся вместе, целиком. Сумма прижата к правому
 * краю, не уже столбца сумм и набрана одной строкой: от миллиарда она
 * переносилась посреди числа.
 */
@Composable
private fun PositionFigures(
    position: Position,
    onStorno: () -> Unit,
    onExcise: () -> Unit,
    onRemove: () -> Unit
) {
    WrapRow(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = positionDetail(position),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = DETAIL_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).widthIn(min = KassaLayout.positionDetail)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            MoneyText(Money.format(position.total), Modifier.widthIn(min = TableColumns.money))
            PositionActions(position, onStorno, onExcise, onRemove)
        }
    }
}

/**
 * Марки, сторно и удаление строки.
 *
 * Сторно отменяет уже пробитую строку и остаётся в чеке; удаление
 * убирает строку до пробития. Сторнированную сторнировать нечем.
 * Марки считываются после того, как товар уже в чеке: кассир
 * пробивает бутылку, потом подносит к сканеру её марку.
 */
@Composable
private fun PositionActions(
    position: Position,
    onStorno: () -> Unit,
    onExcise: () -> Unit,
    onRemove: () -> Unit
) {
    val texts = LocalStrings.current
    IconButton(enabled = !position.storno, onClick = onExcise) {
        Icon(AppIcons.excise, contentDescription = LocalSaleTexts.current.excise)
    }
    IconButton(onClick = onStorno) {
        Icon(
            imageVector = AppIcons.storno,
            contentDescription = if (position.storno) texts.sale.stornoUndo else texts.sale.storno
        )
    }
    IconButton(onClick = onRemove) {
        Icon(AppIcons.remove, contentDescription = texts.sale.remove)
    }
}

/** Сколько строк отдаётся наименованию в листе чека. */
private const val NAME_LINES = 2

/** Столько же — строке о количестве, цене и ставке. */
private const val DETAIL_LINES = 2
