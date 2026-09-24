package kz.mybrain.superkassa.data.analytics

import kz.mybrain.superkassa.domain.analytics.model.SalesDay
import kz.mybrain.superkassa.domain.analytics.model.SalesDelivery
import kz.mybrain.superkassa.domain.analytics.model.SalesDeliveryCounts
import kz.mybrain.superkassa.domain.analytics.model.SalesFigures
import kz.mybrain.superkassa.domain.analytics.model.SalesFilter
import kz.mybrain.superkassa.domain.analytics.model.SalesHour
import kz.mybrain.superkassa.domain.analytics.model.SalesPayments
import kz.mybrain.superkassa.domain.analytics.model.SalesSummary
import kz.mybrain.superkassa.domain.analytics.model.SalesUnit
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetDecimal
import kz.mybrain.superkassa.integrations.bfdcabinet.analytics.SalesDay as WireDay
import kz.mybrain.superkassa.integrations.bfdcabinet.analytics.SalesDelivery as WireDelivery
import kz.mybrain.superkassa.integrations.bfdcabinet.analytics.SalesDeliveryCounts as WireCounts
import kz.mybrain.superkassa.integrations.bfdcabinet.analytics.SalesFigures as WireFigures
import kz.mybrain.superkassa.integrations.bfdcabinet.analytics.SalesFilter as WireFilter
import kz.mybrain.superkassa.integrations.bfdcabinet.analytics.SalesHour as WireHour
import kz.mybrain.superkassa.integrations.bfdcabinet.analytics.SalesPayments as WirePayments
import kz.mybrain.superkassa.integrations.bfdcabinet.analytics.SalesSummary as WireSummary
import kz.mybrain.superkassa.integrations.bfdcabinet.analytics.SalesUnit as WireUnit

/**
 * Торговая сводка кабинета — в предметную область.
 *
 * Кабинет пишет деньги десятичными тенге; здесь они целые тиыны, как и
 * у ядра кассы: запись числа переводит сам модуль кабинета, без двоичной
 * дроби. Незаполненная сумма остаётся незаполненной — прочерком на экране,
 * а не нулём.
 */

internal fun SalesFilter.wire(): WireFilter = WireFilter(from, to, retailPlaceId, cashRegisterId)

internal fun WireFigures.domain(): SalesFigures = SalesFigures(
    summary = summary.domain(),
    days = days.map { it.domain() },
    hours = hours.map { it.domain() },
    registers = registers.map { it.domain() },
    places = places.map { it.domain() },
    delivery = delivery.domain()
)

internal fun WireSummary.domain(): SalesSummary = SalesSummary(
    receiptCount = receiptCount,
    revenue = revenue.tiynOrNull(),
    refunds = refunds.tiynOrNull(),
    difference = difference.tiynOrNull(),
    averageReceipt = averageReceipt.tiynOrNull(),
    tax = tax.tiynOrNull(),
    payments = payments.domain(),
    offlineCount = offlineCount,
    queuedCount = queuedCount,
    unknownCount = unknownCount,
    cashRegisterCount = cashRegisterCount,
    openShiftCount = openShiftCount,
    purchaseCount = purchaseCount,
    purchases = purchases.tiynOrNull(),
    purchaseRefunds = purchaseRefunds.tiynOrNull()
)

private fun WireDay.domain(): SalesDay =
    SalesDay(date, receiptCount, revenue.tiynOrNull(), refunds.tiynOrNull(), difference.tiynOrNull())

private fun WireHour.domain(): SalesHour = SalesHour(hour, receiptCount, revenue.tiynOrNull())

private fun WirePayments.domain(): SalesPayments = SalesPayments(
    cash = cash.tiynOrNull(),
    card = card.tiynOrNull(),
    electronic = electronic.tiynOrNull(),
    mobile = mobile.tiynOrNull(),
    credit = credit.tiynOrNull(),
    tare = tare.tiynOrNull(),
    other = other.tiynOrNull()
)

private fun WireUnit.domain(): SalesUnit = SalesUnit(
    id = id,
    registrationNumber = registrationNumber,
    name = name,
    retailPlaceName = retailPlaceName,
    receiptCount = receiptCount,
    revenue = revenue.tiynOrNull(),
    difference = difference.tiynOrNull(),
    lastContactAt = lastContactAt
)

private fun WireDelivery.domain(): SalesDelivery = SalesDelivery(receipts.domain(), reports.domain(), offlineCount)

private fun WireCounts.domain(): SalesDeliveryCounts = SalesDeliveryCounts(total, delivered, queued, unknown, rejected)

/** Сумма в тиынах; не пришла — не пришла. */
private fun CabinetDecimal?.tiynOrNull(): Long? = this?.tiyn()
