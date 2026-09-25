package kz.mybrain.superkassa.presentation.kassa.sale.position

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
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.text.MoneyText
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.TableColumns
import kz.mybrain.superkassa.domain.kassa.model.sale.Position
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.kassa.sale.LocalSaleTexts

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
                text = position.label(texts.receipt.storno),
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
 * Состав тянется на остаток ряда и обрезается многоточием; сумма
 * и действия стоят справа целиком. Сумма прижата к правому краю, шириной
 * столбца сумм — одного на весь чек, чтобы суммы строк стояли столбцом, —
 * и набрана одной строкой: от миллиарда она переносилась посреди числа.
 */
@Composable
private fun PositionFigures(
    position: Position,
    onStorno: () -> Unit,
    onExcise: () -> Unit,
    onRemove: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = positionDetail(position),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = DETAIL_LINES,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            MoneyText(Money.formatTiyn(position.total), Modifier.widthIn(min = TableColumns.money))
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
            contentDescription = if (position.storno) texts.receipt.stornoUndo else texts.receipt.storno
        )
    }
    IconButton(onClick = onRemove) {
        Icon(AppIcons.remove, contentDescription = texts.receipt.remove)
    }
}

/** Сколько строк отдаётся наименованию в листе чека. */
private const val NAME_LINES = 2

/** Столько же — строке о количестве, цене и ставке. */
private const val DETAIL_LINES = 2
