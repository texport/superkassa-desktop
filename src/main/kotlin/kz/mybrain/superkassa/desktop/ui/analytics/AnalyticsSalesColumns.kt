package kz.mybrain.superkassa.desktop.ui.analytics

import kz.mybrain.superkassa.desktop.server.cabinet.SalesUnit
import kz.mybrain.superkassa.desktop.server.cabinet.orZero
import kz.mybrain.superkassa.desktop.ui.cabinet.cabinetMoment
import kz.mybrain.superkassa.desktop.ui.cabinet.cabinetSum
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs

/** Что показывает таблица сводки: кассы компании или её торговые точки. */
enum class SalesRows { Registers, Places }

/** Столбец таблицы сводки. */
enum class SalesColumn { Name, RegistrationNumber, RetailPlace, Receipts, Revenue, Net, LastContact }

/**
 * Столбцы под то, что таблица показывает.
 *
 * Таблица одна на оба разреза — считается в них одно и то же, — но
 * столбцы у них разные. Номер КГД выдан кассе, и торговая точка — та,
 * в которой касса стоит; у самой точки нет ни того, ни другого, и оба
 * столбца стояли в её таблице сплошными прочерками во всю ширину
 * экрана. Набор столбцов теперь зависит от того, что в строках.
 */
fun salesColumns(rows: SalesRows): List<SalesColumn> = when (rows) {
    SalesRows.Registers -> listOf(
        SalesColumn.Name,
        SalesColumn.RegistrationNumber,
        SalesColumn.RetailPlace,
        SalesColumn.Receipts,
        SalesColumn.Revenue,
        SalesColumn.Net,
        SalesColumn.LastContact
    )
    SalesRows.Places -> listOf(
        SalesColumn.Name,
        SalesColumn.Receipts,
        SalesColumn.Revenue,
        SalesColumn.Net,
        SalesColumn.LastContact
    )
}

/**
 * Как подписан столбец.
 *
 * У таблицы точек первый столбец назван точкой: в ней он единственный
 * словесный, и подпись «Название» над списком магазинов ничего не говорит.
 */
fun salesColumnTitle(column: SalesColumn, rows: SalesRows, texts: AnalyticsTexts): String = when (column) {
    SalesColumn.Name -> if (rows == SalesRows.Registers) texts.sales.colName else texts.retailPlace
    SalesColumn.RegistrationNumber -> texts.registrationNumber
    SalesColumn.RetailPlace -> texts.retailPlace
    SalesColumn.Receipts -> texts.sales.receipts
    SalesColumn.Revenue -> texts.sales.revenue
    SalesColumn.Net -> texts.sales.net
    SalesColumn.LastContact -> texts.lastContact
}

/** Что стоит в клетке строки; недостающее — прочерком, а не пустотой. */
fun salesCellValue(column: SalesColumn, row: SalesUnit): String = when (column) {
    SalesColumn.Name -> unitTitle(row)
    SalesColumn.RegistrationNumber -> row.registrationNumber ?: Glyphs.DASH
    SalesColumn.RetailPlace -> unitPlace(row)
    SalesColumn.Receipts -> row.receiptCount.toString()
    SalesColumn.Revenue -> cabinetSum(row.revenue)
    SalesColumn.Net -> cabinetSum(row.difference)
    SalesColumn.LastContact -> cabinetMoment(row.lastContactAt)
}

/** По каким столбцам таблицу выстраивают; по остальным порядок не меняют. */
fun salesSortOrder(column: SalesColumn): SalesOrder? = when (column) {
    SalesColumn.Receipts -> SalesOrder.Receipts
    SalesColumn.Revenue -> SalesOrder.Revenue
    else -> null
}

/** Денежный столбец: такой набирается моноширинным, как во всём приложении. */
fun salesMoneyColumn(column: SalesColumn): Boolean =
    column == SalesColumn.Revenue || column == SalesColumn.Net

/** По какому столбцу выстроена таблица сводки. */
enum class SalesOrder { Revenue, Receipts }

/**
 * Порядок строк таблицы.
 *
 * По убыванию выручки с самого начала: таблицу открывают, чтобы увидеть,
 * кто торгует больше всех, а не чтобы прочесть её целиком.
 */
data class SalesSort(val by: SalesOrder = SalesOrder.Revenue, val descending: Boolean = true) {

    /** Нажатие на свой столбец переворачивает порядок, на чужой — переносит его. */
    fun toggled(column: SalesOrder): SalesSort =
        if (column == by) copy(descending = !descending) else SalesSort(column)
}

/** Строки в выбранном порядке. */
fun sortedUnits(rows: List<SalesUnit>, sort: SalesSort): List<SalesUnit> {
    val ordered = when (sort.by) {
        SalesOrder.Revenue -> rows.sortedBy { it.revenue.orZero() }
        SalesOrder.Receipts -> rows.sortedBy { it.receiptCount }
    }
    return if (sort.descending) ordered.asReversed() else ordered
}
