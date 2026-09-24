package kz.mybrain.superkassa.integrations.bfdcabinet.register

import io.ktor.http.ContentType
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.CabinetLink
import kz.mybrain.superkassa.integrations.bfdcabinet.transport.inPath

/**
 * Регистрационная карта кассы и её версии.
 *
 * Карту выдаёт КГД после постановки на учёт; кабинет хранит её, печатает
 * в PDF и помнит, какой она была до каждой перерегистрации.
 */
class CardsApi internal constructor(private val link: CabinetLink) {

    /** Действующая карта. */
    suspend fun card(registerId: String): RegistrationCard = link.get(base(registerId))

    /** Действующая карта в PDF. */
    suspend fun cardPdf(registerId: String): ByteArray =
        link.bytes("${base(registerId)}/pdf", ContentType.Application.Pdf)

    /** Версии карты; кабинет отдаёт их голым массивом, а разбор берёт любой. */
    suspend fun versions(registerId: String): List<RegistrationCardVersion> = link.rows("${base(registerId)}/versions")

    /** Одна версия в том виде, в каком она тогда действовала. */
    suspend fun version(registerId: String, version: Int): RegistrationCard =
        link.get("${base(registerId)}/versions/$version")

    /** PDF нужной версии. */
    suspend fun versionPdf(registerId: String, version: Int): ByteArray =
        link.bytes("${base(registerId)}/versions/$version/pdf", ContentType.Application.Pdf)

    private fun base(registerId: String) = "/api/cash-registers/${registerId.inPath()}/registration-card"
}
