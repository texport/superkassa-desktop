package kz.mybrain.superkassa.desktop.server.cabinet

import io.ktor.http.ContentType
import io.ktor.http.HttpMethod

/**
 * Регистрационная карта кассы и её версии.
 *
 * Карту выдаёт КГД после постановки на учёт; кабинет хранит её, печатает
 * в PDF и помнит, какой она была до каждой перерегистрации.
 */

suspend fun CabinetClient.registrationCard(token: String, id: String): RegistrationCard =
    request(HttpMethod.Get, "/api/cash-registers/$id/registration-card", token = token)

suspend fun CabinetClient.registrationCardPdf(token: String, id: String): ByteArray =
    bytes("/api/cash-registers/$id/registration-card/pdf", ContentType.Application.Pdf, token)

/**
 * Версии карты: чем открыта, чем закрыта и что в ней менялось.
 *
 * Кабинет отдаёт их голым массивом, а не страницей: версий у кассы
 * за её жизнь немного. Разбор общий и снисходительный — конверт
 * частью договора не считается.
 */
suspend fun CabinetClient.registrationCardVersions(
    token: String,
    id: String
): List<RegistrationCardVersion> =
    rows(token, "/api/cash-registers/$id/registration-card/versions")

/** Одна версия целиком: та же карта, но в том виде, в каком она тогда действовала. */
suspend fun CabinetClient.registrationCardVersion(token: String, id: String, version: Int): RegistrationCard =
    request(HttpMethod.Get, "/api/cash-registers/$id/registration-card/versions/$version", token = token)

/** PDF нужной версии: им и подтверждают, где касса стояла в те дни. */
suspend fun CabinetClient.registrationCardVersionPdf(token: String, id: String, version: Int): ByteArray =
    bytes(
        "/api/cash-registers/$id/registration-card/versions/$version/pdf",
        ContentType.Application.Pdf,
        token
    )
