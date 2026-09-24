package kz.mybrain.superkassa.domain.cabinet.port

import kz.mybrain.superkassa.domain.cabinet.model.RegistrationCardVersion
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegistrationCard

/** Регистрационная карта кассы и её версии. */
interface CabinetCards {
    suspend fun card(registerId: String): RegistrationCard

    suspend fun cardPdf(registerId: String): ByteArray

    suspend fun versions(registerId: String): List<RegistrationCardVersion>

    suspend fun version(registerId: String, version: Int): RegistrationCard

    suspend fun versionPdf(registerId: String, version: Int): ByteArray
}
