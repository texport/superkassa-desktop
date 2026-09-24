package kz.mybrain.superkassa.presentation.kassa.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.domain.kassa.model.sale.Basket
import kz.mybrain.superkassa.presentation.common.adaptive.TwoPane
import kz.mybrain.superkassa.presentation.common.list.ScrollableColumn
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.kassa.sale.component.BasketCard
import kz.mybrain.superkassa.presentation.kassa.sale.component.CustomerDataCard
import kz.mybrain.superkassa.presentation.kassa.sale.component.DomainCard
import kz.mybrain.superkassa.presentation.kassa.sale.component.ExciseDialog
import kz.mybrain.superkassa.presentation.kassa.sale.component.IssueRow
import kz.mybrain.superkassa.presentation.kassa.sale.component.PaymentCard
import kz.mybrain.superkassa.presentation.kassa.sale.component.ReceiptChangesCard
import kz.mybrain.superkassa.presentation.kassa.sale.component.ReceiptTotals
import kz.mybrain.superkassa.presentation.kassa.sale.component.SaleHeader
import kz.mybrain.superkassa.presentation.kassa.sale.entry.PositionEntryCard
import kz.mybrain.superkassa.presentation.kassa.sale.position.LocalUnits
import kz.mybrain.superkassa.presentation.kassa.sale.position.LocalVatRates
import kz.mybrain.superkassa.presentation.kassa.sale.position.measureUnits
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.kassa.saleTexts
import kz.mybrain.superkassa.presentation.theme.size.KassaLayout
import kz.mybrain.superkassa.presentation.theme.size.Panes
import kz.mybrain.superkassa.presentation.theme.size.Spacing

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
fun SaleScreen(model: SaleViewModel) {
    val state by model.state.collectAsScreenState()
    val actions = remember(model) { model.actions() }
    // Смену открывают на главном, отрасль выбирают в настройках: экран,
    // открытый снова, узнаёт их заново, а корзину не трогает.
    LaunchedEffect(model) { model.visit() }
    SaleContent(state, actions)
}

/** Экран продажи по готовому состоянию: снимки вида рисуют его без модели. */
@Composable
fun SaleContent(state: SaleUiState, actions: SaleActions = SaleActions()) {
    val texts = LocalStrings.current
    val language = LocalLanguage.current
    CompositionLocalProvider(
        LocalSaleTexts provides saleTexts(language),
        LocalVatRates provides state.positionVat(language, texts.enums),
        LocalUnits provides measureUnits(language)
    ) {
        TwoPane(
            split = Panes.receiptAndTill,
            modifier = Modifier.fillMaxSize().padding(Spacing.screen),
            first = { ReceiptColumn(state, actions) },
            second = { TillColumn(state, actions) }
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
private fun ReceiptColumn(state: SaleUiState, actions: SaleActions) {
    // Какой позиции считывают марки: окно открывается поверх листа чека
    // и живёт, пока кассир подносит к сканеру одну бутылку за другой —
    // и после поворота экрана тоже.
    var stamping by rememberSaveable { mutableStateOf<Int?>(null) }
    stamping?.let { at -> StampDialog(state.basket, at, actions.basket) { stamping = null } }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.normal)
    ) {
        SaleHeader(state.form.operation, state.basket.positions.isNotEmpty(), actions)
        BasketCard(
            basket = state.basket,
            modifier = Modifier.weight(1f),
            onStorno = actions.basket::storno,
            onExcise = { stamping = it },
            onRemove = actions.basket::remove
        )
    }
}

/** Марки одной строки; строка пропала из чека — окну закрыться. */
@Composable
private fun StampDialog(basket: Basket, at: Int, actions: BasketActions, onClose: () -> Unit) {
    val position = basket.positions.getOrNull(at)
    if (position == null) {
        onClose()
        return
    }
    ExciseDialog(stamps = position.exciseStamps, onChanged = { actions.stamp(at, it) }, onDismiss = onClose)
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
private fun TillColumn(state: SaleUiState, actions: SaleActions) {
    val toggle = actions.toggle
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.normal)
    ) {
        ScrollableColumn(modifier = Modifier.weight(1f), gutter = KassaLayout.tillGutter) {
            // Штрихкод стоит первым: сканер вводит код в поле, которое
            // кассир видит, а реквизиты отрасли прежде уводили его под
            // сгиб. Незаполненный реквизит назовёт строка под кнопкой.
            PositionEntryCard(state, actions.entry, state.expanded(SalePanel.PositionEntry)) {
                toggle(SalePanel.PositionEntry)
            }
            DomainCard(state.domainKind, state.form.domain, actions.form::domain)
            PaymentCard(state, actions.payments, state.expanded(SalePanel.Money)) { toggle(SalePanel.Money) }
            TillExtras(state, actions)
        }
        // Прибитое стоит в тех же полях, что и прокручиваемое над ним.
        Column(
            modifier = Modifier.padding(end = KassaLayout.tillGutter),
            verticalArrangement = Arrangement.spacedBy(Spacing.normal)
        ) {
            ReceiptTotals(state.form, state.total, state.expanded(SalePanel.Money), actions.form::taken)
            IssueRow(state, actions.issue)
        }
    }
}

/** Скидки и данные покупателя: нужны не в каждом чеке и стоят последними. */
@Composable
private fun TillExtras(state: SaleUiState, actions: SaleActions) {
    ReceiptChangesCard(state, actions.form, state.expanded(SalePanel.ReceiptChanges)) {
        actions.toggle(SalePanel.ReceiptChanges)
    }
    CustomerDataCard(state.form, actions.form, state.expanded(SalePanel.CustomerData)) {
        actions.toggle(SalePanel.CustomerData)
    }
}
