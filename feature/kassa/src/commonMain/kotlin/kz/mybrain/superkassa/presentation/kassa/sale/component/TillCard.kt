package kz.mybrain.superkassa.presentation.kassa.sale.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.theme.size.Spacing

/**
 * Карточка раздела кассы: ввод позиции, реквизиты отрасли, оплата,
 * скидки, данные покупателя.
 *
 * Одна на все разделы колонки кассы: пять одинаковых копий `Card` с одним
 * и тем же отступом расходились бы при первой правке одной из них.
 * Залитая, а не обводная ([kz.mybrain.superkassa.designsystem.section.CollapsibleCard]):
 * разделы кассы стоят вплотную столбиком, и рамки у каждого дробили бы
 * колонку на клетки.
 */
@Composable
internal fun TillCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
            content = content
        )
    }
}
