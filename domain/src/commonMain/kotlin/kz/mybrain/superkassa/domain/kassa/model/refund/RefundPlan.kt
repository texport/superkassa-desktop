package kz.mybrain.superkassa.domain.kassa.model.refund

import io.github.texport.superkassa.core.domain.api.model.common.Decimal
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import io.github.texport.superkassa.core.presentation.api.model.receipt.CreateReceiptCommand
import io.github.texport.superkassa.core.presentation.api.model.receipt.CustomerContactRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ParentTicketRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptDomainRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemRequest
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptItemView
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptPaymentRequest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kz.mybrain.superkassa.domain.kassa.model.Tenge
import kz.mybrain.superkassa.domain.kassa.model.entry.QUANTITY_SCALE
import kotlin.time.Instant

/**
 * Что уходит в кассу чеком возврата, кроме ключа.
 *
 * @property lines отмеченные строки чека-основания и доля каждой в тиынах;
 *   пусто — возврат суммой, одной строкой.
 * @property lineName как назвать единственную строку возврата суммой.
 * @property domain отрасль кассы без подблока: номер машины и время
 *   стоянки принадлежат чеку-основанию, а пустой реквизит на их месте —
 *   выдуманный реквизит в фискальном документе.
 * @property contact контакт покупателя, по которому ему уходит чек возврата;
 *   `null` — чек покупателю не отправляется.
 */
data class RefundPlan(
    val kind: ReturnKind,
    val basis: FiscalDocumentResponse,
    val kgdKkmId: String,
    val refundTiyn: Long,
    val lines: List<Pair<ReceiptItemView, Long>>,
    val lineName: String,
    val payments: List<ReceiptPaymentRequest>,
    val domain: ReceiptDomainRequest,
    val contact: CustomerContactRequest? = null
) {
    /** Основание описано целиком: без номера или суммы возврат по нему не оформить. */
    val complete: Boolean get() = basis.docNo != null && basis.totalAmount != null
}

/**
 * Чек возврата от чека-основания.
 *
 * Отмеченные строки уходят своими строками — с наименованием, ценой,
 * количеством и ставкой проданного, — и каждая стоит ровно своей доли
 * итога: скидка на весь проданный чек ложится на строки возврата скидкой
 * на позицию. Без отметок — одна строка на сумму возврата, и ставку
 * касса берёт у чека-основания.
 *
 * `parentTicketTotal` — итог чека-основания целиком, а не сумма возврата:
 * это реквизит того чека, по которому возврат оформляется.
 *
 * @return `null` — у основания нет номера или суммы, и возврат по нему
 *   не оформить.
 */
internal fun RefundPlan.command(kkmId: String, pin: String, key: String): CreateReceiptCommand? {
    val parent = parentTicket() ?: return null
    return CreateReceiptCommand(
        kkmId = kkmId,
        pin = pin,
        operation = kind.operation.name,
        idempotencyKey = key,
        items = lines.map { (item, share) -> refundLine(item, share) }.ifEmpty { listOf(sumLine()) },
        discountPercent = null,
        discountSum = null,
        markupPercent = null,
        markupSum = null,
        payments = payments,
        taken = null,
        parentTicket = parent,
        domain = domain,
        customerContact = contact
    )
}

/** Реквизиты чека-основания; `null` — без номера или суммы его не описать. */
private fun RefundPlan.parentTicket(): ParentTicketRequest? {
    val number = basis.docNo
    val total = basis.totalAmount
    return if (number == null || total == null) {
        null
    } else {
        ParentTicketRequest(
            parentTicketNumber = number,
            parentTicketDateTime = ticketMoment(basis.createdAt),
            kgdKkmId = kgdKkmId,
            parentTicketTotal = Tenge.decimal(total),
            parentTicketIsOffline = basis.isAutonomous
        )
    }
}

/** Возврат суммой — одна строка на всю сумму; ставку касса берёт у основания. */
private fun RefundPlan.sumLine() =
    ReceiptItemRequest(name = lineName, price = Tenge.decimal(refundTiyn), quantity = ONE)

/**
 * Строка чека-основания, возвращаемая за свою долю.
 *
 * Касса считает строку ценой на количество к ближайшему тиыну; разница
 * до доли уходит скидкой на позицию, а если доля больше — наценкой.
 */
private fun refundLine(item: ReceiptItemView, share: Long): ReceiptItemRequest {
    val quantity = Decimal.ofScaled(item.quantityThousandths, QUANTITY_SCALE)
    val gap = Tenge.lineSum(Tenge.of(item.price), quantity) - share
    return ReceiptItemRequest(
        name = item.name,
        nameKk = item.nameKk,
        price = item.price,
        quantity = quantity,
        vatGroup = item.vatGroup,
        measureUnitCode = item.measureUnitCode,
        barcode = item.barcode,
        discountSum = gap.takeIf { it > 0L }?.let(Tenge::decimal),
        markupSum = gap.takeIf { it < 0L }?.let { Tenge.decimal(-it) }
    )
}

private val ONE = Decimal.ofScaled(1, 0)

/**
 * Время чека-основания в запросе возврата: `2026-09-07T14:42:08` по UTC.
 *
 * Это не показ, а поле протокола: его вид задан БФД, пояс — нулевой.
 */
internal fun ticketMoment(millis: Long): String =
    TICKET_MOMENT.format(Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC))

private val TICKET_MOMENT = LocalDateTime.Format {
    date(LocalDate.Formats.ISO)
    char('T')
    hour()
    char(':')
    minute()
    char(':')
    second()
}
