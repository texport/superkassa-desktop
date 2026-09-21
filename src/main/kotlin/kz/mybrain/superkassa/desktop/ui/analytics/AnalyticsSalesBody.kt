package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.components.ScrollableColumn
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.EnumStrings
import kz.mybrain.superkassa.desktop.ui.strings.HistoryJournalTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Сама сводка: плитки, графики, таблицы и доставка.
 *
 * Порядок отвечает порядку вопросов владельца: сколько всего — сколько
 * скуплено у населения — как это расходится по дням — чем платили —
 * когда покупают — кто торгует — всё ли доехало до ОФД. Каждый раздел
 * взят в карточку раздела, общую на всё приложение, и своей разметки
 * не заводит.
 *
 * Карточки покупки нет вовсе, пока за срок ничего не скуплено: у кассы,
 * которая скупкой не занимается, она стояла бы рядом нулями каждый день.
 *
 * Прокрутка одна на весь столбец: ни один раздел внутри не прокручивается
 * сам, иначе колесо над таблицей двигало бы её, а не страницу.
 */
@Composable
fun AnalyticsSalesBody(
    view: SalesView,
    texts: AnalyticsTexts,
    enums: EnumStrings,
    journal: HistoryJournalTexts,
    cabinet: CabinetTexts,
    modifier: Modifier = Modifier
) {
    val sales = texts.sales
    ScrollableColumn(modifier = modifier, spacing = Spacing.snug) {
        SalesTiles(view.summary, texts)
        if (view.summary.purchased) {
            SectionCard(cabinet.operationPurchase, info = sales.purchasesHint) {
                SalesPurchaseTiles(view.summary, sales, cabinet)
            }
        }
        SectionCard(sales.byDay, info = sales.byDayHint) {
            SalesChart(dayBars(view.days, view.range, sales), sales)
        }
        SectionCard(sales.payments, info = sales.paymentsHint) {
            SalesShares(salesShares(view.summary.payments, enums, sales.paymentOther), sales)
        }
        SectionCard(sales.byHour, info = sales.byHourHint) {
            SalesChart(hourBars(view.hours, sales), sales)
        }
        SectionCard(sales.registers, info = sales.registersHint) {
            SalesTable(view.registers, SalesRows.Registers, texts, journal, sales.allRegistersShown)
        }
        SectionCard(sales.places, info = sales.placesHint) {
            SalesTable(view.places, SalesRows.Places, texts, journal, sales.allPlacesShown)
        }
        SectionCard(sales.delivery, info = sales.deliveryHint) {
            SalesDeliveryTiles(view.delivery, sales)
        }
    }
}
