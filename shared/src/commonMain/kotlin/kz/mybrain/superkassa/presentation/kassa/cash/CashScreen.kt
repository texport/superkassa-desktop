package kz.mybrain.superkassa.presentation.kassa.cash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.common.adaptive.CardColumns
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.common.list.ScrollableColumn
import kz.mybrain.superkassa.presentation.common.message.InfoTip
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.section.ScreenTitle
import kz.mybrain.superkassa.presentation.common.strings.LocalLanguage
import kz.mybrain.superkassa.presentation.common.strings.LocalStrings
import kz.mybrain.superkassa.presentation.common.text.MoneyText
import kz.mybrain.superkassa.presentation.kassa.cash.component.CashForm
import kz.mybrain.superkassa.presentation.kassa.cash.component.RecentCash
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.presentation.theme.type.MoneyStyle
import kz.mybrain.superkassa.strings.api.kassa.DrawerTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Денежный ящик: остаток, внесение, изъятие и что уже проведено.
 *
 * Остаток берётся из счётчика кассы — того же, по которому касса решает,
 * хватает ли денег на изъятие. Свой подсчёт по документам разошёлся бы
 * с ним, и касса отказывала бы в изъятии денег, которые по её же экрану
 * в ящике есть.
 */
@Composable
fun CashScreen(model: CashViewModel) {
    val state by model.state.collectAsScreenState()
    LaunchedEffect(model) { model.visit() }
    CashContent(state, model)
}

/** Денежный ящик по готовому состоянию: снимки вида рисуют его без модели. */
@Composable
fun CashContent(state: CashUiState, actions: CashActions = object : CashActions {}) {
    val money = textsOf(LocalLanguage.current).kassa.money
    // Экран во всю ширину раздела: на широком окне остаток с формой стоят
    // слева, проведённое — справа, и половина экрана не пустует.
    ScrollableColumn(modifier = Modifier.fillMaxSize(), spacing = Spacing.cardGap) {
        // Заголовок — имя раздела, как у продажи и возврата: «В денежном
        // ящике» стояло и заголовком, и подписью остатка под ним.
        ScreenTitle(LocalStrings.current.sections.cash)
        CardColumns(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.cardGap)) {
                DrawerCard(state.cashInDrawer, money.drawer)
                CashForm(state, actions, money)
            }
            RecentCash(state, money.drawer, actions::rereadRecent)
        }
    }
}

/**
 * Остаток ящика — главное число экрана.
 *
 * Подпись сверху, сумма под ней крупно и вправо, объяснение снизу: кассир
 * читает остаток с метра, а объяснение — только когда сумма его удивила.
 * Состояние смены здесь не повторяется: оно стоит плашкой в шапке окна,
 * одной на все разделы.
 */
@Composable
private fun DrawerCard(cashInDrawer: Long?, drawer: DrawerTexts) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.blockPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.itemGap)
        ) {
            DrawerLabel(drawer)
            // Тем же начертанием, что плитка на главном экране: одно число
            // в двух местах набиралось по-разному. Одной строкой и целиком.
            MoneyText(Money.formatTiyn(cashInDrawer), Modifier.fillMaxWidth(), MoneyStyle.hero)
            // Пока остаток неизвестен, об этом сказано строкой: это помеха.
            // Объяснение, кто его считает, ушло под значок у подписи.
            if (cashInDrawer == null) {
                Text(
                    text = drawer.unknownBalance,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Подпись остатка; кто его считает — под значком: читать это в каждой смене незачем. */
@Composable
private fun DrawerLabel(drawer: DrawerTexts) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = drawer.inDrawer,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        InfoTip(drawer.countedByNode)
    }
}
