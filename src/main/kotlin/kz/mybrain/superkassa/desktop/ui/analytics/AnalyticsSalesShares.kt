package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.ui.cabinet.cabinetSum
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsSalesTexts
import kz.mybrain.superkassa.desktop.ui.theme.ChartColors
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

/** Полоса долей и подписи под ней. */
@Composable
fun SalesShares(shares: List<SalesShare>, texts: AnalyticsSalesTexts, modifier: Modifier = Modifier) {
    if (shares.isEmpty()) {
        Footnote(texts.noPayments)
        return
    }
    val colors = ChartColors.shares
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.tight)
    ) {
        ShareBar(shares, colors)
        shares.forEachIndexed { at, share -> ShareRow(share, colors[at % colors.size]) }
    }
}

/**
 * Сама полоса: доли идут подряд, в том же порядке, что и подписи.
 *
 * Полотно, а не ряд из `Box` с весами: вес меньше сотой доли Compose
 * округляет до нуля, и доля в полпроцента пропадала из полосы целиком,
 * оставаясь в подписях.
 */
@Composable
private fun ShareBar(shares: List<SalesShare>, colors: List<Color>) {
    val total = shares.fold(BigDecimal.ZERO) { sum, share -> sum + share.amount }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(Sizes.shareBar)
            .clip(MaterialTheme.shapes.extraSmall)
    ) {
        var left = 0f
        shares.forEachIndexed { at, share ->
            val width = size.width * barShare(share.amount, total)
            drawRect(
                color = colors[at % colors.size],
                topLeft = Offset(left, 0f),
                size = Size(width, size.height)
            )
            left += width
        }
    }
}

/**
 * Неразрывный пробел перед знаком процента.
 *
 * Задан escape-последовательностью, как и разделитель разрядов в суммах:
 * в исходнике он неотличим от обычного пробела, и однажды такая подмена
 * уже разводила показ и разбор.
 */

/** Подпись доли: цвет, название, доля в процентах и сумма. */
@Composable
private fun ShareRow(share: SalesShare, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Swatch(color)
        Text(
            text = share.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "${share.percent}${Glyphs.NBSP}%",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = cabinetSum(share.amount), style = MoneyStyle.caption)
    }
}

/** Цветной образец доли: он связывает подпись с куском полосы. */
@Composable
private fun Swatch(color: Color) {
    Box(
        modifier = Modifier
            .size(Sizes.shareSwatch)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(color)
    )
}
