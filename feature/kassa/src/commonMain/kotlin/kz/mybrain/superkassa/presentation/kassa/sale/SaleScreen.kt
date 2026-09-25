package kz.mybrain.superkassa.presentation.kassa.sale

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import kz.mybrain.superkassa.designsystem.adaptive.SupportingPanes
import kz.mybrain.superkassa.designsystem.list.ScrollableColumn
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.size.KassaLayout
import kz.mybrain.superkassa.designsystem.theme.size.Panes
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.kassa.model.sale.Basket
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.kassa.sale.component.BasketCard
import kz.mybrain.superkassa.presentation.kassa.sale.component.CheckoutPanel
import kz.mybrain.superkassa.presentation.kassa.sale.component.CustomerDataCard
import kz.mybrain.superkassa.presentation.kassa.sale.component.DomainCard
import kz.mybrain.superkassa.presentation.kassa.sale.component.ExciseDialog
import kz.mybrain.superkassa.presentation.kassa.sale.component.IssueRow
import kz.mybrain.superkassa.presentation.kassa.sale.component.IssuedCard
import kz.mybrain.superkassa.presentation.kassa.sale.component.ReceiptChangesCard
import kz.mybrain.superkassa.presentation.kassa.sale.component.ReceiptTotals
import kz.mybrain.superkassa.presentation.kassa.sale.component.SaleHeader
import kz.mybrain.superkassa.presentation.kassa.sale.entry.PositionEntryCard
import kz.mybrain.superkassa.presentation.kassa.sale.position.LocalUnits
import kz.mybrain.superkassa.presentation.kassa.sale.position.LocalVatRates
import kz.mybrain.superkassa.presentation.kassa.sale.position.measureUnits
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Продажа и покупка.
 *
 * Экран разложен на две панели, как товароучётный терминал: чек — он
 * главный и забирает всё место, кроме кассы, — и касса, где кассир вводит
 * позицию, выбирает оплату и видит итог. Касса не шире своего предела
 * ([Panes.receiptAndTill]): в окне по умолчанию она прежде стояла постоянной
 * ширины и оставляла названию товара в чеке одну букву.
 *
 * Касса — вспомогательная панель ([SupportingPanes]): на широком окне
 * она справа от чека, где рядом тесно — телефон, планшет стоймя, — лежит
 * снизу нижним листом, который тянут за ручку до итога с «Пробить чек».
 * Разделы кассы, и «К оплате» с оплатой в нём, сворачиваются вниз
 * стрелками в своих заголовках; что развёрнуто, помнит рабочее место.
 *
 * Одна операция на два направления намеренно: состав чека у продажи
 * и покупки одинаковый, различается только направление денег.
 */
@Composable
fun SaleScreen(model: SaleViewModel, output: ReceiptOutput = ReceiptOutput()) {
    val state by model.state.collectAsScreenState()
    val actions = remember(model) { model.actions() }
    // Смену открывают на главном, отрасль выбирают в настройках: экран,
    // открытый снова, узнаёт их заново, а корзину не трогает.
    LaunchedEffect(model) { model.visit() }
    SaleContent(state, actions, output)
}

/** Экран продажи по готовому состоянию: снимки вида рисуют его без модели. */
@Composable
fun SaleContent(state: SaleUiState, actions: SaleActions = SaleActions(), output: ReceiptOutput = ReceiptOutput()) {
    val texts = LocalStrings.current
    val language = LocalLanguage.current
    CompositionLocalProvider(
        LocalSaleTexts provides textsOf(language).kassa.sale,
        LocalVatRates provides state.positionVat(language, texts.enums),
        LocalUnits provides measureUnits(language)
    ) {
        SupportingPanes(
            split = Panes.receiptAndTill,
            expanded = state.expanded(SalePanel.Till),
            onToggle = { actions.toggle(SalePanel.Till) },
            main = { ReceiptColumn(state, actions, output) },
            supporting = { TillBody(state, actions) },
            summary = { Checkout(state, actions) },
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Левая колонка — сам чек.
 *
 * Список позиций забирает всю оставшуюся высоту: кассир смотрит в него
 * чаще, чем во всё остальное вместе взятое. Пока следующий чек пуст,
 * над ним стоит итог пробитого: сумма, сдача, показ и печать.
 */
@Composable
private fun ReceiptColumn(state: SaleUiState, actions: SaleActions, output: ReceiptOutput) {
    // Какой позиции считывают марки: окно открывается поверх листа чека
    // и живёт, пока кассир подносит к сканеру одну бутылку за другой —
    // и после поворота экрана тоже.
    var stamping by rememberSaveable { mutableStateOf<Int?>(null) }
    stamping?.let { at -> StampDialog(state.basket, at, actions.basket) { stamping = null } }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)
    ) {
        state.shownIssued?.let { IssuedCard(it, output, actions.next) }
        BasketCard(
            basket = state.basket,
            modifier = Modifier.weight(1f),
            onStorno = actions.basket::storno,
            onExcise = { stamping = it },
            onRemove = actions.basket::remove
        ) { SaleHeader(state.form.operation, state.basket.positions.size, actions) }
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
 * Касса — рабочее место кассира: ввод, реквизиты и оплата прокручиваются.
 *
 * Разделы сворачиваются: высота колонки одна, и кассир отдаёт её тому,
 * чем занят сейчас.
 */
@Composable
private fun TillBody(state: SaleUiState, actions: SaleActions) {
    val toggle = actions.toggle
    ScrollableColumn(modifier = Modifier.fillMaxSize(), gutter = KassaLayout.tillGutter) {
        // Штрихкод стоит первым: сканер вводит код в поле, которое
        // кассир видит, а реквизиты отрасли прежде уводили его под
        // сгиб. Незаполненный реквизит назовёт строка под кнопкой.
        PositionEntryCard(state, actions.entry, state.expanded(SalePanel.PositionEntry)) {
            toggle(SalePanel.PositionEntry)
        }
        DomainCard(state.domainKind, state.form.domain, actions.form::domain)
        TillExtras(state, actions)
    }
}

/**
 * «К оплате» с единственной кнопкой — то, без чего чек не пробить: сумма
 * к оплате и «Пробить чек» на экране всегда, сколько бы позиций ни
 * набралось. Оплата — в том же блоке и сворачивается вниз вместе с ним.
 */
@Composable
private fun Checkout(state: SaleUiState, actions: SaleActions) {
    // Итог стоит в тех же полях, что и прокручиваемое над ним.
    Box(modifier = Modifier.padding(end = KassaLayout.tillGutter)) {
        CheckoutPanel {
            ReceiptTotals(
                state = state,
                payments = actions.payments,
                expanded = state.expanded(SalePanel.Money),
                onToggle = { actions.toggle(SalePanel.Money) },
                onTaken = actions.form::taken,
                modifier = Modifier.weight(1f, fill = false)
            )
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
    CustomerDataCard(state.form, state.channels, actions.form, state.expanded(SalePanel.CustomerData)) {
        actions.toggle(SalePanel.CustomerData)
    }
}
