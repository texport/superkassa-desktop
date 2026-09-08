package kz.mybrain.superkassa.desktop.server.cabinet

import io.ktor.http.ContentType
import io.ktor.http.HttpMethod

/**
 * Заявления в ИСНА и регистрационная карта кассы.
 *
 * Все три заявления идут одним путём: кабинет готовит то, что нужно
 * подписать, владелец подписывает ЭЦП, кабинет отправляет подписанное.
 */

// --- Регистрационные действия ---

suspend fun CabinetClient.prepareRegistration(token: String, id: String): ApplicationPrepared =
    request(HttpMethod.Post, "/api/cash-registers/$id/registration/application", token = token)

suspend fun CabinetClient.signRegistration(token: String, id: String, sign: SignRequest): ApplicationSent =
    request(HttpMethod.Post, "/api/cash-registers/$id/registration/sign", sign, token)

suspend fun CabinetClient.prepareReregistration(
    token: String,
    id: String,
    application: ReregistrationRequest
): ApplicationPrepared =
    request(HttpMethod.Post, "/api/cash-registers/$id/reregistration/application", application, token)

suspend fun CabinetClient.signReregistration(token: String, id: String, sign: SignRequest): ApplicationSent =
    request(HttpMethod.Post, "/api/cash-registers/$id/reregistration/sign", sign, token)

suspend fun CabinetClient.prepareDeregistration(
    token: String,
    id: String,
    application: DeregistrationRequest
): ApplicationPrepared =
    request(HttpMethod.Post, "/api/cash-registers/$id/deregistration/application", application, token)

suspend fun CabinetClient.signDeregistration(token: String, id: String, sign: SignRequest): ApplicationSent =
    request(HttpMethod.Post, "/api/cash-registers/$id/deregistration/sign", sign, token)

suspend fun CabinetClient.registrationActions(token: String, id: String): CabinetPage<RegistrationAction> =
    request(HttpMethod.Get, "/api/cash-registers/$id/registration-actions?page=0&size=$PAGE_SIZE", token = token)

suspend fun CabinetClient.registrationAction(token: String, id: String, actionId: String): RegistrationAction =
    request(HttpMethod.Get, "/api/cash-registers/$id/registration-actions/$actionId", token = token)

// --- Регистрационная карта ---

suspend fun CabinetClient.registrationCard(token: String, id: String): RegistrationCard =
    request(HttpMethod.Get, "/api/cash-registers/$id/registration-card", token = token)

suspend fun CabinetClient.registrationCardPdf(token: String, id: String): ByteArray =
    bytes("/api/cash-registers/$id/registration-card/pdf", ContentType.Application.Pdf, token)
