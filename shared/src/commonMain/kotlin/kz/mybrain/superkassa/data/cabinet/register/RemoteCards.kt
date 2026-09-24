package kz.mybrain.superkassa.data.cabinet.register

import kz.mybrain.superkassa.data.cabinet.cabinetCall
import kz.mybrain.superkassa.domain.cabinet.model.RegistrationCardVersion
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegistrationCard
import kz.mybrain.superkassa.domain.cabinet.port.CabinetCards
import kz.mybrain.superkassa.integrations.bfdcabinet.register.CardsApi
import kz.mybrain.superkassa.integrations.bfdcabinet.register.RegistrationCard as BfdCard
import kz.mybrain.superkassa.integrations.bfdcabinet.register.RegistrationCardVersion as BfdVersion

/** Регистрационные карты касс — модулем кабинета от имени вошедшего. */
internal class RemoteCards(private val cards: CardsApi) : CabinetCards {
    override suspend fun card(registerId: String): RegistrationCard = cabinetCall { cards.card(registerId) }.card()

    override suspend fun cardPdf(registerId: String): ByteArray = cabinetCall { cards.cardPdf(registerId) }

    override suspend fun versions(registerId: String): List<RegistrationCardVersion> =
        cabinetCall { cards.versions(registerId) }.map { it.version() }

    override suspend fun version(registerId: String, version: Int): RegistrationCard =
        cabinetCall { cards.version(registerId, version) }.card()

    override suspend fun versionPdf(registerId: String, version: Int): ByteArray =
        cabinetCall { cards.versionPdf(registerId, version) }
}

private fun BfdCard.card() = RegistrationCard(
    cashRegisterId = cashRegisterId,
    status = status,
    companyBin = companyBin,
    companyName = companyName,
    retailPlaceName = retailPlaceName,
    address = address,
    rka = rka,
    cato = cato,
    modelName = modelName,
    factoryNumber = factoryNumber,
    kkmId = kkmId,
    registrationNumber = registrationNumber,
    lastSuccessfulAction = lastSuccessfulAction,
    updatedAt = updatedAt,
    deregisteredAt = deregisteredAt,
    deregistrationReason = deregistrationReason
)

private fun BfdVersion.version() =
    RegistrationCardVersion(version, status, validFrom, validTo, openedBy, closedBy, changed, current)
