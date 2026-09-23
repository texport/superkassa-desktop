package kz.mybrain.superkassa.desktop.tools

import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.refreshSelected
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.ParentTicket
import kz.mybrain.superkassa.desktop.server.ReceiptItem
import kz.mybrain.superkassa.desktop.server.ReceiptPayment
import kz.mybrain.superkassa.desktop.server.ReceiptRequest
import kz.mybrain.superkassa.desktop.server.buy
import kz.mybrain.superkassa.desktop.server.buyReturn
import kz.mybrain.superkassa.desktop.server.sell
import kz.mybrain.superkassa.desktop.server.sellReturn
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.sale.DomainInput
import kz.mybrain.superkassa.desktop.ui.sale.DomainKind
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Чеки всех видов с полным наполнением.
 *
 * Наполнение намеренно предельное: несколько позиций с разными ставками,
 * позиционная скидка, сторно, скидка на чек, БИН покупателя и отраслевые
 * реквизиты. Обычный чек проходит и без этого — ломается редкое.
 */
object ReceiptScenarios {

    suspend fun all(session: Session, kkmId: String, report: CycleReport) {
        sellFull(session, kkmId, report)
        sellByDomains(session, kkmId, report)
        buyFull(session, kkmId, report)
        returns(session, kkmId, report)
    }

    private suspend fun sellFull(session: Session, kkmId: String, report: CycleReport) {
        val request = ReceiptRequest(
            idempotencyKey = key("sell-full"),
            items = listOf(
                ReceiptItem("Хлеб", BigDecimal("250.0"), BigDecimal("2.0"), "VAT_16", discountSum = BigDecimal("25.0")),
                ReceiptItem("Молоко", BigDecimal("480.5"), BigDecimal("1.0"), "VAT_10"),
                ReceiptItem("Книга", BigDecimal("3200.0"), BigDecimal("1.0"), "NO_VAT"),
                ReceiptItem("Возврат позиции", BigDecimal("200.0"), BigDecimal("1.0"), "VAT_16", isStorno = true)
            ),
            payments = listOf(ReceiptPayment("CASH", BigDecimal("3755.5"))),
            discountSum = BigDecimal("200.0"),
            taken = BigDecimal("4000.0"),
            customerBin = "960624350642"
        )
        report.step(session, "Продажа с полным наполнением") {
            session.client.sell(kkmId, request, FullCycle.WORK_PIN)
        }
    }

    /**
     * По одному чеку на каждый вид отрасли.
     *
     * Отрасль стоит настройкой кассы, поэтому проба её и переключает:
     * проверяется тот самый путь, которым идёт касса — настройка, поля
     * выбранной отрасли, один подблок в запросе. После прогона настройка
     * возвращается к прежней: рабочее место не должно остаться стоянкой.
     */
    private suspend fun sellByDomains(session: Session, kkmId: String, report: CycleReport) {
        val chosen = session.domain
        DOMAINS.forEach { (kind, requisites) ->
            session.chooseDomain(kkmId, kind)
            val request = ReceiptRequest(
                idempotencyKey = key("sell-${kind.name.lowercase()}"),
                items = listOf(ReceiptItem(kind.code, BigDecimal("1500.0"), BigDecimal("1.0"), "VAT_16")),
                payments = listOf(ReceiptPayment("CARD", BigDecimal("1500.0"))),
                domain = requisites.toDomain(kind)
            )
            report.step(session, "Продажа: ${kind.code}") {
                session.client.sell(kkmId, request, FullCycle.WORK_PIN)
            }
        }
        session.chooseDomain(kkmId, chosen)
    }

    private suspend fun buyFull(session: Session, kkmId: String, report: CycleReport) {
        val request = ReceiptRequest(
            idempotencyKey = key("buy-full"),
            items = listOf(ReceiptItem("Приём тары", BigDecimal("120.0"), BigDecimal("10.0"), "NO_VAT")),
            payments = listOf(ReceiptPayment("CASH", BigDecimal("1200.0"))),
            taken = BigDecimal("1200.0")
        )
        report.step(session, "Покупка") { session.client.buy(kkmId, request, FullCycle.WORK_PIN) }
    }

    /** Возвраты оформляются от настоящего чека смены, а не от выдуманного. */
    private suspend fun returns(session: Session, kkmId: String, report: CycleReport) {
        session.refreshSelected()
        val parent = session.documents.firstOrNull {
            it.docType == "CHECK" && it.docNo != null && it.totalAmount != null
        }
        if (parent == null) {
            report.fail("Возвраты", "в смене нет чека-основания")
            return
        }
        report.step(session, "Возврат продажи") {
            session.client.sellReturn(kkmId, refundOf(parent, session, "sell-return"), FullCycle.WORK_PIN)
        }
        report.step(session, "Возврат покупки") {
            session.client.buyReturn(kkmId, refundOf(parent, session, "buy-return"), FullCycle.WORK_PIN)
        }
    }

    private fun refundOf(parent: Document, session: Session, tag: String): ReceiptRequest {
        val total = Money.tengeOf(parent.totalAmount ?: 0L)
        return ReceiptRequest(
            idempotencyKey = key(tag),
            items = listOf(ReceiptItem("Возврат по чеку ${parent.docNo}", total, BigDecimal("1.0"), "VAT_16")),
            payments = listOf(ReceiptPayment("CASH", total)),
            taken = total,
            parentTicket = ParentTicket(
                parentTicketNumber = parent.docNo ?: 0L,
                parentTicketDateTime = MOMENT.format(Instant.ofEpochMilli(parent.createdAt ?: 0L)),
                kgdKkmId = session.selected?.kkmKgdId.orEmpty(),
                parentTicketTotal = total,
                parentTicketIsOffline = parent.isAutonomous == true
            )
        )
    }

    private val MOMENT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC)

    private fun key(tag: String) = "cycle-$tag-${System.currentTimeMillis()}"

    /** Отрасли и реквизиты, которых каждая из них требует. */
    private val DOMAINS = listOf(
        DomainKind.Services to DomainInput(accountNumber = "ACC-1024"),
        DomainKind.Hotels to DomainInput(accountNumber = "ROOM-317"),
        DomainKind.GasOil to DomainInput(cardNumber = "CARD-77"),
        DomainKind.Taxi to DomainInput(carNumber = "123ABC", isOrder = true, currentFee = "350"),
        DomainKind.Parking to DomainInput(parkingFrom = "8:15", parkingTo = "10:15")
    )
}
