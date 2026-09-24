package kz.mybrain.superkassa.data.cabinet.documents

import kz.mybrain.superkassa.data.cabinet.toDecimal
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetCashMovement
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetReceipt
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetReport
import kz.mybrain.superkassa.domain.cabinet.model.documents.CabinetShift
import kz.mybrain.superkassa.domain.cabinet.model.documents.DocumentsOverview
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegistrationCardSummary
import kz.mybrain.superkassa.integrations.bfdcabinet.documents.CabinetCashMovement as BfdMovement
import kz.mybrain.superkassa.integrations.bfdcabinet.documents.CabinetReceipt as BfdReceipt
import kz.mybrain.superkassa.integrations.bfdcabinet.documents.CabinetReport as BfdReport
import kz.mybrain.superkassa.integrations.bfdcabinet.documents.CabinetShift as BfdShift
import kz.mybrain.superkassa.integrations.bfdcabinet.documents.DocumentsOverview as BfdOverview

/** Строки списков документов и сводка по кассе — видом предметной области. */
internal fun BfdOverview.overview() = DocumentsOverview(
    cashRegisterId = cashRegisterId,
    registrationCard = registrationCard?.let {
        RegistrationCardSummary(it.available, it.status, it.pdfStatus, it.updatedAt)
    },
    receiptsCount = receiptsCount,
    reportsCount = reportsCount,
    shiftsCount = shiftsCount,
    cashMovementsCount = cashMovementsCount
)

internal fun BfdReceipt.receipt() = CabinetReceipt(
    transactionId = transactionId,
    receiptNumber = receiptNumber,
    shiftNumber = shiftNumber,
    operationType = operationType,
    total = total?.toDecimal(),
    createdAt = createdAt,
    sendStatus = sendStatus,
    deliveryStatus = deliveryStatus,
    kgdMark = kgdMark,
    kgdMarkAt = kgdMarkAt
)

internal fun BfdShift.shift() = CabinetShift(
    shiftNumber = shiftNumber,
    state = state,
    openedAt = openedAt,
    closedAt = closedAt,
    receiptsCount = receiptsCount,
    saleTotal = saleTotal?.toDecimal(),
    returnTotal = returnTotal?.toDecimal(),
    buyTotal = buyTotal?.toDecimal(),
    buyReturnTotal = buyReturnTotal?.toDecimal(),
    cashBalance = cashBalance?.toDecimal(),
    cashTotal = cashTotal?.toDecimal()
)

internal fun BfdReport.report() = CabinetReport(
    transactionId = transactionId,
    type = type,
    shiftNumber = shiftNumber,
    createdAt = createdAt,
    total = total?.toDecimal(),
    sendStatus = sendStatus,
    deliveryStatus = deliveryStatus
)

internal fun BfdMovement.movement() = CabinetCashMovement(
    transactionId = transactionId,
    type = type,
    amount = amount?.toDecimal(),
    shiftNumber = shiftNumber,
    createdAt = createdAt,
    kkmTime = kkmTime,
    sendStatus = sendStatus
)
