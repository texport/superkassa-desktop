package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.ScrollableColumn
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.SaleTexts
import kz.mybrain.superkassa.desktop.ui.strings.saleTexts
import kz.mybrain.superkassa.desktop.ui.strings.saleTextsKk
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.math.BigDecimal

/**
 * Надписи области продажи на языке кассира.
 *
 * Экран кладёт их в контекст один раз, и части экрана берут строку
 * по смыслу, не зная выбранного языка.
 */
val LocalSaleTexts: ProvidableCompositionLocal<SaleTexts> = staticCompositionLocalOf { saleTextsKk }

/**
 * Продажа и покупка.
 *
 * Экран разложен на две колонки, как товароучётный терминал: слева чек —
 * он главный и занимает всё оставшееся место, справа узкая кассовая
 * колонка, где кассир вводит позицию, выбирает оплату и видит итог.
 * Раньше всё шло одним прокручиваемым столбцом, и итог с кнопкой уезжали
 * под сгиб ровно тогда, когда чек становился длинным.
 *
 * Одна операция на два направления намеренно: состав чека у продажи
 * и покупки одинаковый, различается только направление денег.
 */
@Composable
fun SaleScreen(session: Session) {
    val basket = remember { Basket() }
    val form = remember { SaleForm() }
    val panels = remember { SalePanels(session.preferences) }
    val total = totalOf(basket, form)
    val texts = LocalStrings.current
    CompositionLocalProvider(
        LocalSaleTexts provides saleTexts(session.language),
        LocalVatRates provides vatRatesOf(session, texts.enums),
        LocalUnits provides session.units
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(Spacing.screen),
            horizontalArrangement = Arrangement.spacedBy(Spacing.roomy)
        ) {
            ReceiptColumn(form, basket, Modifier.weight(1f))
            TillColumn(session, form, basket, panels, total)
        }
    }
}

/**
 * Левая колонка — сам чек.
 *
 * Список позиций забирает всю оставшуюся высоту: кассир смотрит в него
 * чаще, чем во всё остальное вместе взятое.
 */
@Composable
private fun ReceiptColumn(form: SaleForm, basket: Basket, modifier: Modifier) {
    // Какой позиции считывают марки: окно открывается поверх листа чека
    // и живёт, пока кассир подносит к сканеру одну бутылку за другой.
    var stamping by remember { mutableStateOf<Int?>(null) }
    stamping?.let { at ->
        val position = basket.positions.getOrNull(at)
        if (position == null) {
            stamping = null
        } else {
            ExciseDialog(
                stamps = position.exciseStamps,
                onChanged = { basket.stampAt(at, it) }
            ) { stamping = null }
        }
    }
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(Spacing.normal)
    ) {
        SaleHeader(form, basket)
        BasketCard(
            basket = basket,
            modifier = Modifier.weight(1f),
            onStorno = { basket.stornoAt(it) },
            onExcise = { stamping = it },
            onRemove = { basket.removeAt(it) }
        )
    }
}

/**
 * Правая колонка — рабочее место кассира.
 *
 * Ввод и оплата прокручиваются, итог с единственной кнопкой прибиты
 * к низу: сумма к оплате и «Пробить чек» обязаны быть на экране всегда,
 * сколько бы позиций ни набралось.
 *
 * Разделы сворачиваются: высота колонки одна, и кассир отдаёт её тому,
 * чем занят сейчас.
 */
@Composable
private fun TillColumn(
    session: Session,
    form: SaleForm,
    basket: Basket,
    panels: SalePanels,
    total: BigDecimal
) {
    Column(
        modifier = Modifier.width(TILL_WIDTH).fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(Spacing.normal)
    ) {
        // Прокручивается ввод и реквизиты, а не деньги: «Итого» и «Пробить
        // чек» кассир видит в каждом чеке. Обратный порядок пробовался —
        // ввод позиции переставал прокручиваться, зато под сгиб уезжала
        // главная кнопка экрана, и это хуже.
        ScrollableColumn(modifier = Modifier.weight(1f)) {
            PositionEntryCard(
                session = session,
                expanded = panels.expanded(SalePanel.PositionEntry),
                onToggle = { panels.toggle(SalePanel.PositionEntry) }
            ) { basket.add(it) }
            ReceiptChangesCard(
                session = session,
                form = form,
                basket = basket,
                expanded = panels.expanded(SalePanel.ReceiptChanges),
                onToggle = { panels.toggle(SalePanel.ReceiptChanges) }
            )
            CustomerDataCard(
                form = form,
                expanded = panels.expanded(SalePanel.CustomerData),
                onToggle = { panels.toggle(SalePanel.CustomerData) }
            )
        }
        // Оплата и итог прибиты к низу вместе с кнопкой: их видят в каждом
        // чеке, и уезжать под сгиб они не имеют права.
        ReceiptTotals(
            session = session,
            form = form,
            total = total,
            expanded = panels.expanded(SalePanel.Money),
            onToggle = { panels.toggle(SalePanel.Money) }
        )
        IssueRow(session, basket, form)
    }
}

/**
 * Ширина кассовой колонки.
 *
 * Считается из тех же полей, что в ней стоят: самая широкая строка —
 * поле реквизита рядом с ценой, плюс место под количество. Число здесь
 * развело бы ширину колонки и ширину её содержимого.
 *
 * Шире, чем нужно самой широкой строке, намеренно: плашки видов оплаты
 * при узкой колонке переносились на четыре строки и съедали высоту,
 * которой не хватало вводу позиции. По ширине место есть — лист чека
 * рядом всё равно наполовину пуст.
 */
private val TILL_WIDTH = Sizes.fieldForm + Sizes.fieldPrice + Sizes.fieldQuantity
