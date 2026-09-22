package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.SaleTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

/**
 * Строка корзины.
 *
 * Сторно окрашено ролью ошибки целиком, а не помечено одним словом: строка,
 * уводящая итог вниз, обязана отличаться от продажи с одного взгляда.
 *
 * Нажатие на саму карточку открывает подробности строки; значки справа
 * перехватывают своё нажатие и до карточки его не доносят, поэтому
 * сторно и удаление работают как прежде.
 */
@Composable
internal fun PositionCard(
    position: Position,
    onOpen: () -> Unit,
    onStorno: () -> Unit,
    onExcise: () -> Unit,
    onRemove: () -> Unit
) {
    val texts = LocalStrings.current
    OutlinedCard(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            colors = positionColors(position.storno),
            // Ни наименование, ни состав строки не растут больше чем
            // на две строки: у товара с каталожным именем в шесть слов
            // карточка вырастала до шести строк, и в лист чека помещалось
            // четыре позиции. Целиком имя, количество, цену и ставку
            // показывает окно подробностей — оно открывается нажатием
            // по самой карточке.
            headlineContent = {
                Text(
                    text = position.label(texts.sale.storno),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = NAME_LINES,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Text(
                    text = positionDetail(position),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = DETAIL_LINES,
                    overflow = TextOverflow.Ellipsis
                )
            },
            trailingContent = { PositionSum(position, onStorno, onExcise, onRemove) }
        )
    }
}

/** Цвета строки: обычная берёт роли поверхности, сторно — роли ошибки. */
@Composable
private fun positionColors(storno: Boolean) = if (storno) {
    ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        headlineColor = MaterialTheme.colorScheme.onErrorContainer,
        supportingColor = MaterialTheme.colorScheme.onErrorContainer,
        trailingIconColor = MaterialTheme.colorScheme.onErrorContainer
    )
} else {
    ListItemDefaults.colors()
}

/** Сумма строки моноширинным столбцом и удаление позиции. */
@Composable
private fun PositionSum(
    position: Position,
    onStorno: () -> Unit,
    onExcise: () -> Unit,
    onRemove: () -> Unit
) {
    val texts = LocalStrings.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = Money.format(position.total),
            style = MoneyStyle.row,
            modifier = Modifier.width(Sizes.fieldAmount)
        )
        // Сторно отменяет уже пробитую строку и остаётся в чеке; удаление
        // убирает строку до пробития. Сторнированную сторнировать нечем.
        // Марки считываются после того, как товар уже в чеке: кассир
        // пробивает бутылку, потом подносит к сканеру её марку.
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
}

/** Из чего сложилась строка: количество, цена за единицу, ставка и скидка. */
@Composable
private fun positionDetail(position: Position): String {
    // Единица стоит при количестве: «1,5 × 2 500» и «1,5 кг × 2 500» —
    // разные строки чека, и кассир обязан видеть которая перед ним.
    val unit = unitTitle(LocalUnits.current, position.measureUnitCode)
    // Ставка в строке стоит, только когда касса её выделяет: у
    // неплательщика НДС «Без НДС» в каждой строке чека — шум, а не
    // сведения, и раньше на этом месте стояла ставка, до БФД не дошедшая.
    val rates = LocalVatRates.current
    val vat = if (rates.size < 2) null else vatTitle(rates, position.vatGroup)
    return positionLine(position, unit, vat, LocalSaleTexts.current)
}

/**
 * Состав строки словами — без Compose: разбирается проверкой, а не глазом.
 *
 * Сведения разделяет общий знак набора, а не набранная здесь точка:
 * своя копия давала в листе чека другой зазор, чем в соседних списках
 * приложения, и первая же правка общего знака обошла бы эту строку.
 */
internal fun positionLine(
    position: Position,
    unit: String,
    vat: String?,
    texts: SaleTexts
): String {
    // Количество отделяет дробь тем же знаком, что и деньги: строка
    // «1.450 кг × 3 450,00 ₸» разводила в одном месте две записи числа.
    val counted = listOf(quantityText(position.quantity), unit).filter { it.isNotBlank() }
    val parts = mutableListOf(
        counted.joinToString(" ") + Glyphs.TIMES + Money.format(position.price)
    )
    vat?.let { parts += it }
    if (position.discount > BigDecimal.ZERO) {
        parts += "${texts.lineDiscount} ${Money.format(position.discount)}"
    }
    // Число после слова, а не перед ним: «1 марок» — согласование, которое
    // по-русски верно только при пяти и больше, а марка бывает и одна.
    if (position.exciseStamps.isNotEmpty()) {
        parts += "${texts.exciseCount}: ${position.exciseStamps.size}"
    }
    return parts.joinToString(Glyphs.SEPARATOR)
}

/** Количество товара словами кассира: дробь через запятую, как и в суммах. */
internal fun quantityText(quantity: BigDecimal): String =
    quantity.toPlainString().replace('.', Glyphs.DECIMAL)

/** Сколько строк отдаётся наименованию в листе чека. */
private const val NAME_LINES = 2

/** Столько же — строке о количестве, цене и ставке. */
private const val DETAIL_LINES = 2
