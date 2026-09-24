package kz.mybrain.superkassa.domain.journal.port

/**
 * Что журналу нужно снаружи сверх кассы: доставка чека покупателю.
 *
 * Собирается в точке сборки платформы; касса процесса отдаёт доставку
 * отдельным от кассовых операций фасадом.
 */
class JournalPorts(val deliveries: ReceiptDeliveries)
