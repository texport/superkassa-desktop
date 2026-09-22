package kz.mybrain.superkassa.desktop.ui.returns

import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.ParentTicket
import kz.mybrain.superkassa.desktop.server.ReceiptItem
import kz.mybrain.superkassa.desktop.server.ReceiptPayment
import kz.mybrain.superkassa.desktop.server.ReceiptRequest
import kz.mybrain.superkassa.desktop.server.SoldItem
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.sale.QUANTITY_SCALE
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Что не так с введённой суммой возврата — на языке кассира, а не разбора. */
enum class RefundProblem { Empty, NotANumber, NotPositive, TooLarge }

/** Проверенная сумма возврата: либо тиыны, либо причина отказа. */
sealed interface RefundAmount {
    data class Ready(val tiyn: Long) : RefundAmount
    data class Rejected(val reason: RefundProblem) : RefundAmount
}

/**
 * Проверяет сумму возврата против суммы чека-основания.
 *
 * Возврат части чека — обычное дело: покупатель вернул один товар из трёх.
 * Больше, чем было в чеке, вернуть нельзя, и узнавать об этом из отказа
 * ОФД кассир не должен — проверка идёт до отправки.
 */
fun refundAmountOf(entered: String, basisTiyn: Long): RefundAmount {
    if (entered.isBlank()) return RefundAmount.Rejected(RefundProblem.Empty)
    val parsed = Money.parse(entered) ?: return RefundAmount.Rejected(RefundProblem.NotANumber)
    val tiyn = parsed.movePointRight(Money.TIYN_SCALE).toLong()
    if (tiyn <= 0L) return RefundAmount.Rejected(RefundProblem.NotPositive)
    if (tiyn > basisTiyn) return RefundAmount.Rejected(RefundProblem.TooLarge)
    return RefundAmount.Ready(tiyn)
}

/** Сумма чека-основания из журнала в том виде, в каком её принимает поле. */
fun tengeText(tiyn: Long): String = Money.entered(Money.tengeOf(tiyn))

/**
 * Наличных в ящике меньше, чем возвращают деньгами.
 *
 * Возврат продажи отдаёт деньги из того же ящика, из которого их изымают,
 * и о нехватке кассир должен узнать до того, как назовёт сумму покупателю.
 * Возврат покупки деньги принимает — ему хватает всегда. Неизвестный
 * остаток молчит: утверждать нехватку по неизвестному числу нельзя.
 *
 * @return остаток ящика, когда его не хватает, иначе `null`.
 */
fun drawerShortage(kind: ReturnKind, drawerTiyn: Long?, cashRefund: BigDecimal): Long? {
    if (kind != ReturnKind.Sell) return null
    val drawer = drawerTiyn ?: return null
    return drawer.takeIf { Money.tiynOf(cashRefund) > it }
}

/**
 * Сумма перечисленных позиций в тиынах.
 *
 * Одна на весь возврат: ею заполняется поле суммы при отметке позиций,
 * ею же решается, уйдут ли позиции строками чека. Разойдись эти два
 * счёта — отметки попали бы в чек с чужой суммой.
 */
fun itemsTiyn(items: List<SoldItem>): Long =
    items.fold(BigDecimal.ZERO) { sum, item -> sum + item.sum }
        .movePointRight(Money.TIYN_SCALE)
        .toLong()

/**
 * Собирает чек возврата от чека-основания.
 *
 * Одной строкой: узел не отдаёт позиции чека-основания, и придумывать их
 * заново значило бы отправить в ОФД товары, которых в чеке не было.
 * Ставка НДС не указывается — узел берёт ставку кассы. Прежде здесь стояла
 * жёстко вписанная VAT_16, и на кассе без НДС она врала в налоге.
 *
 * `parentTicketTotal` — сумма чека-основания целиком, а не сумма возврата:
 * это реквизит того чека, по которому возврат оформляется.
 */
fun refundRequest(
    basis: Document,
    kgdKkmId: String,
    refundTiyn: Long,
    idempotencyKey: String,
    lineName: String,
    payments: List<ReceiptPayment>,
    returned: List<SoldItem> = emptyList()
): ReceiptRequest? {
    val number = basis.docNo ?: return null
    val basisTotal = basis.totalAmount ?: return null
    val refund = Money.tengeOf(refundTiyn)
    // Позиции уходят строками только тогда, когда сумма возврата — это
    // в точности их сумма. Кассир вправе поправить поле после отметок,
    // и тогда строки чека описывали одну сумму, а оплата — другую:
    // такой чек ОФД принять не может.
    val lines = returned.takeIf { itemsTiyn(it) == refundTiyn }.orEmpty()
    return ReceiptRequest(
        idempotencyKey = idempotencyKey,
        // Отмеченные позиции уходят своими строками: ОФД получает то же
        // наименование, цену и ставку, что были в проданном чеке. Пустой
        // выбор означает возврат суммой — одной строкой, как раньше.
        items = lines.ifEmpty { null }?.map { item ->
            ReceiptItem(
                name = item.name,
                nameKk = item.nameKk,
                price = item.price,
                quantity = BigDecimal.valueOf(item.quantityThousandths, QUANTITY_SCALE),
                vatGroup = item.vatGroup,
                measureUnitCode = item.measureUnitCode
            )
        } ?: listOf(ReceiptItem(name = lineName, price = refund, quantity = BigDecimal("1.0"))),
        payments = payments,
        parentTicket = ParentTicket(
            parentTicketNumber = number,
            parentTicketDateTime = TICKET_MOMENT.format(Instant.ofEpochMilli(basis.createdAt ?: 0L)),
            kgdKkmId = kgdKkmId,
            parentTicketTotal = Money.tengeOf(basisTotal),
            parentTicketIsOffline = basis.isAutonomous == true
        )
    )
}

private val TICKET_MOMENT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss").withZone(ZoneOffset.UTC)
