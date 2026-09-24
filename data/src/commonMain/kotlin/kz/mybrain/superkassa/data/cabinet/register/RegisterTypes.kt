package kz.mybrain.superkassa.data.cabinet.register

import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.CashRegisterModel
import kz.mybrain.superkassa.domain.cabinet.model.LastRegistrationAction
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlaceRef
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegisterState
import kz.mybrain.superkassa.domain.cabinet.model.documents.TechnicalState
import kz.mybrain.superkassa.integrations.bfdcabinet.register.CabinetRegister as BfdRegister
import kz.mybrain.superkassa.integrations.bfdcabinet.register.RegisterState as BfdState
import kz.mybrain.superkassa.integrations.bfdcabinet.register.TechnicalState as BfdTechnical

/**
 * Касса кабинета — кассой предметной области.
 *
 * Модель и точку модуль уже свёл к одному виду из объектов карточки
 * и плоских полей списка; здесь они только переписываются.
 */
internal fun BfdRegister.register() = CabinetRegister(
    id = id,
    kkmId = kkmId,
    internalName = internalName,
    status = status,
    registrationNumber = registrationNumber,
    factoryNumber = factoryNumber,
    manufactureYear = manufactureYear,
    model = model?.let { CashRegisterModel(it.modelCode, it.name) },
    retailPlace = retailPlace?.let { RetailPlaceRef(it.id, it.name) },
    lastRegistrationAction = lastRegistrationAction?.let {
        LastRegistrationAction(it.type, it.status, it.at, it.sentAt)
    },
    registrationCardAvailable = registrationCardAvailable
)

/** Состояние кассы по учёту кабинета и по снимку БФД. */
internal fun BfdState.state() =
    RegisterState(cashRegisterId, businessStatus, stateSyncStatus, technicalState?.technical())

private fun BfdTechnical.technical() = TechnicalState(
    found = found,
    active = active,
    inactiveReason = inactiveReason,
    trafficSuspended = trafficSuspended,
    ofdDisconnected = ofdDisconnected,
    billingStatus = billingStatus,
    shiftStatus = shiftStatus,
    shiftNumber = shiftNumber,
    validationMask = validationMask,
    lastContactAt = lastContactAt,
    snapshotAt = snapshotAt
)
