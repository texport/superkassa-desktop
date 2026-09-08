package kz.mybrain.superkassa.desktop.server.cabinet

import io.ktor.http.HttpMethod

/**
 * Кассы кабинета: паспорт, состояние и технический токен.
 *
 * Заявления по этим кассам — в [CabinetRegistrationApi].
 */

// --- Кассы ---

suspend fun CabinetClient.registers(token: String): CabinetPage<CabinetRegister> =
    request(HttpMethod.Get, "/api/cash-registers?page=0&size=$PAGE_SIZE", token = token)

suspend fun CabinetClient.register(token: String, id: String): CabinetRegister =
    request(HttpMethod.Get, "/api/cash-registers/$id", token = token)

suspend fun CabinetClient.addRegister(token: String, register: RegisterCreate): CabinetRegister =
    request(HttpMethod.Post, "/api/cash-registers", register, token)

suspend fun CabinetClient.editRegister(token: String, id: String, edit: RegisterEdit): CabinetRegister =
    request(HttpMethod.Patch, "/api/cash-registers/$id", edit, token)

suspend fun CabinetClient.renameRegister(token: String, id: String, name: String?): CabinetRegister =
    request(HttpMethod.Patch, "/api/cash-registers/$id/internal-name", InternalNameRequest(name), token)

suspend fun CabinetClient.removeRegister(token: String, id: String) {
    call(HttpMethod.Delete, "/api/cash-registers/$id", null, token)
}

suspend fun CabinetClient.registerState(token: String, id: String): RegisterState =
    request(HttpMethod.Get, "/api/cash-registers/$id/state", token = token)

/** Выдаёт кассе новый технический токен. */
suspend fun CabinetClient.issueToken(token: String, id: String): TokenIssued =
    request(HttpMethod.Post, "/api/cash-registers/$id/token", token = token)
