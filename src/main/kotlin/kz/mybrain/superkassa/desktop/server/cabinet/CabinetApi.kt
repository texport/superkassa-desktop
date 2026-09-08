package kz.mybrain.superkassa.desktop.server.cabinet

import io.ktor.http.HttpMethod

/**
 * Вход владельца в кабинет и карточка компании.
 *
 * По одной функции на ручку и ни одной склейки путей на экране: путь
 * написан здесь один раз, а экран называет действие словами предметной
 * области. Точки и справочники — в [CabinetPlacesApi], кассы —
 * в [CabinetRegistersApi], заявления — в [CabinetRegistrationApi],
 * документы — в [CabinetDocumentsApi].
 */

// --- Вход ---

/** Задача на подпись для входа по ЭЦП. */
suspend fun CabinetClient.edsChallenge(): EdsChallenge =
    request(HttpMethod.Post, "/api/auth/eds/challenge")

/** Вход по подписанной задаче. */
suspend fun CabinetClient.edsLogin(challengeId: String, signatureCms: String): CabinetLogin =
    request(HttpMethod.Post, "/api/auth/eds", EdsLoginRequest(challengeId, signatureCms))

/** Кто вошёл по этому доступу. */
suspend fun CabinetClient.me(token: String): CabinetMe =
    request(HttpMethod.Get, "/api/auth/me", token = token)

/** Выход: доступ отзывается на стороне кабинета. */
suspend fun CabinetClient.logout(token: String) {
    call(HttpMethod.Post, "/api/auth/logout", null, token)
}

// --- Компания ---

suspend fun CabinetClient.company(token: String): CompanyProfile =
    request(HttpMethod.Get, "/api/company", token = token)

suspend fun CabinetClient.saveOkeds(token: String, okeds: List<Oked>): OkedsView =
    request(HttpMethod.Put, "/api/company/okeds", OkedsRequest(okeds), token)
