package kz.mybrain.superkassa.desktop.ui.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import kz.mybrain.superkassa.desktop.ui.adaptive.TwoPane
import kz.mybrain.superkassa.desktop.ui.components.ScrollableColumn
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.SaleTexts
import kz.mybrain.superkassa.desktop.ui.strings.saleTexts
import kz.mybrain.superkassa.desktop.ui.strings.saleTextsKk
import kz.mybrain.superkassa.desktop.ui.theme.KassaLayout
import kz.mybrain.superkassa.desktop.ui.theme.Panes
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
 * Экран разложен на две панели, как товароучётный терминал: чек — он
 * главный и забирает всё место, кроме кассы, — и касса, где кассир вводит
 * позицию, выбирает оплату и видит итог. Касса не шире своего предела
 * ([Panes.receiptAndTill]): в окне по умолчанию она прежде стояла постоянной
 * ширины и оставляла названию товара в чеке одну букву. Там, где рядом
 * им тесно, касса встаёт под чеком.
 *
 * Одна операция на два направления намеренно: состав чека у продажи
 * и покупки одинаковый, различается только направление денег.
 */
@Composable
fun SaleScreen(session: Session) {
    SaleWorkplace(session, remember { Basket() }, remember { SaleForm() })
}

/** Экран продажи над готовым чеком: снимки подают его набранным. */
@Composable
internal fun SaleWorkplace(session: Session, basket: Basket, form: SaleForm) {
    val panels = remember { SalePanels(session.preferences) }
    val total = totalOf(basket, form)
    val texts = LocalStrings.current
    CompositionLocalProvider(
        LocalSaleTexts provides saleTexts(session.language),
        LocalVatRates provides vatRatesOf(session, texts.enums),
        LocalUnits provides session.units
    ) {
        TwoPane(
            split = Panes.receiptAndTill,
            modifier = Modifier.fillMaxSize().padding(Spacing.screen),
            first = { ReceiptColumn(form, basket) },
            second = { TillColumn(session, form, basket, panels, total) }
        )
    }
}

/**
 * Левая колонка — сам чек.
 *
 * Список позиций забирает всю оставшуюся высоту: кассир смотрит в него
 * чаще, чем во всё остальное вместе взятое.
 */
@Composable
private fun ReceiptColumn(form: SaleForm, basket: Basket) {
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
        modifier = Modifier.fillMaxSize(),
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
 * Ввод, реквизиты и оплата прокручиваются, итог с единственной кнопкой
 * прибиты к низу: сумма к оплате и «Пробить чек» обязаны быть на экране
 * всегда, сколько бы позиций и оплат ни набралось. Прибито только то,
 * без чего чек не пробить: пять строк оплаты внизу прежде отнимали
 * у ввода всю высоту, и в окне 960×640 поля штрихкода не было вовсе.
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
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.normal)
    ) {
        ScrollableColumn(modifier = Modifier.weight(1f), gutter = KassaLayout.tillGutter) {
            // Штрихкод стоит первым: сканер вводит код в поле, которое
            // кассир видит, а реквизиты отрасли прежде уводили его под
            // сгиб. Незаполненный реквизит назовёт строка под кнопкой.
            PositionEntryCard(
                session = session,
                expanded = panels.expanded(SalePanel.PositionEntry),
                onToggle = { panels.toggle(SalePanel.PositionEntry) }
            ) { basket.add(it) }
            DomainCard(session, form)
            PaymentCard(
                session = session,
                form = form,
                total = total,
                expanded = panels.expanded(SalePanel.Money),
                onToggle = { panels.toggle(SalePanel.Money) }
            )
            TillExtras(session, form, basket, panels)
        }
        // Прибитое стоит в тех же полях, что и прокручиваемое над ним.
        Column(
            modifier = Modifier.padding(end = KassaLayout.tillGutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.normal)
        ) {
            ReceiptTotals(form, total, expanded = panels.expanded(SalePanel.Money))
            IssueRow(session, basket, form)
        }
    }
}

/** Скидки и данные покупателя: нужны не в каждом чеке и стоят последними. */
@Composable
private fun TillExtras(session: Session, form: SaleForm, basket: Basket, panels: SalePanels) {
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
