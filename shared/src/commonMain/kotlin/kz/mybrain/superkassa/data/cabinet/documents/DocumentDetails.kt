package kz.mybrain.superkassa.data.cabinet.documents

import kz.mybrain.superkassa.data.cabinet.toDecimal
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetCashMovementDetails
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetReceiptDetails
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetReportDetails
import kz.mybrain.superkassa.domain.cabinet.model.documents.DocumentItem
import kz.mybrain.superkassa.domain.cabinet.model.documents.DocumentOperator
import kz.mybrain.superkassa.integrations.bfdcabinet.documents.CabinetCashMovementDetails as BfdMovementDetails
import kz.mybrain.superkassa.integrations.bfdcabinet.documents.CabinetReceiptDetails as BfdReceiptDetails
import kz.mybrain.superkassa.integrations.bfdcabinet.documents.CabinetReportDetails as BfdReportDetails
import kz.mybrain.superkassa.integrations.bfdcabinet.documents.DocumentItem as BfdItem

/**
 * Документы целиком — видом предметной области.
 *
 * Пакет протокола модуль уже свёл к тексту: кабинет отдаёт его то объектом,
 * то строкой с тем же объектом внутри.
 */
internal fun BfdReceiptDetails.details() = CabinetReceiptDetails(
    transactionId = transactionId,
    receiptNumber = receiptNumber,
    kkmDocumentNumber = kkmDocumentNumber,
    shiftNumber = shiftNumber,
    operationType = operationType,
    total = total?.toDecimal(),
    taxTotal = taxTotal?.toDecimal(),
    cashTotal = cashTotal?.toDecimal(),
    cardTotal = cardTotal?.toDecimal(),
    createdAt = createdAt,
    registrationNumber = registrationNumber,
    operator = operator?.let { DocumentOperator(it.code, it.name) },
    items = items.map { it.item() },
    sendStatus = sendStatus,
    deliveryStatus = deliveryStatus,
    sentAt = sentAt,
    deliveryResultAt = deliveryResultAt,
    kgdMark = kgdMark,
    kgdMarkAt = kgdMarkAt,
    packet = packet
)

internal fun BfdReportDetails.details() = CabinetReportDetails(
    transactionId = transactionId,
    type = type,
    shiftNumber = shiftNumber,
    createdAt = createdAt,
    total = total?.toDecimal(),
    returnTotal = returnTotal?.toDecimal(),
    buyTotal = buyTotal?.toDecimal(),
    buyReturnTotal = buyReturnTotal?.toDecimal(),
    cashBalance = cashBalance?.toDecimal(),
    receiptsCount = receiptsCount,
    kkmDocumentNumber = kkmDocumentNumber,
    sendStatus = sendStatus,
    deliveryStatus = deliveryStatus,
    packet = packet
)

internal fun BfdMovementDetails.details() = CabinetCashMovementDetails(
    transactionId = transactionId,
    type = type,
    amount = amount?.toDecimal(),
    shiftNumber = shiftNumber,
    createdAt = createdAt,
    protocolDocumentId = protocolDocumentId,
    sendStatus = sendStatus,
    packet = packet
)

private fun BfdItem.item() = DocumentItem(
    positionNumber = positionNumber,
    type = type,
    name = name,
    sectionCode = sectionCode,
    quantity = quantity?.toDecimal(),
    price = price?.toDecimal(),
    sum = sum?.toDecimal(),
    taxPercent = taxPercent?.toDecimal(),
    taxAmount = taxAmount?.toDecimal(),
    measureUnitCode = measureUnitCode,
    barcode = barcode
)
