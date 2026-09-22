package kz.mybrain.superkassa.desktop.server.cabinet

import io.ktor.http.HttpMethod

/**
 * Кассы кабинета: паспорт, состояние и технический токен.
 *
 * Заявления по этим кассам — в [CabinetRegistrationApi].
 */

// --- Кассы ---

suspend fun CabinetClient.registers(token: String, page: Int = 0): CabinetPage<CabinetRegister> =
    request(HttpMethod.Get, "/api/cash-registers?page=$page&size=$PAGE_SIZE", token = token)

/**
 * Все кассы компании.
 *
 * Владелец ищет кассу по номеру КГД и по заводскому среди всех своих,
 * а не среди первых пятидесяти: поиск в колонке точек идёт по прочитанному.
 */
suspend fun CabinetClient.allRegisters(
    token: String,
    onPart: (List<CabinetRegister>, Long) -> Unit = { _, _ -> }
): List<CabinetRegister> = allPages({ page -> registers(token, page) }, onPart)

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
/**
 * Выпуск токена. Кабинет показывает значение только после подтверждения
 * сервиса приёма; не успел — отвечает `PENDING`, и запрос повторяется
 * с тем же ключом идемпотентности, пока подтверждение не придёт.
 */
suspend fun CabinetClient.issueToken(token: String, id: String): TokenIssued {
    val key = java.util.UUID.randomUUID().toString()
    var issued: TokenIssued = requestToken(token, id, key)
    var attempts = 1
    while (issued.pending && attempts < TOKEN_ATTEMPTS) {
        kotlinx.coroutines.delay(TOKEN_RETRY_MS)
        issued = requestToken(token, id, key)
        attempts++
    }
    return issued
}

private suspend fun CabinetClient.requestToken(token: String, id: String, key: String): TokenIssued =
    request(HttpMethod.Post, "/api/cash-registers/$id/token", token = token, idempotencyKey = key)

private const val TOKEN_ATTEMPTS = 10
private const val TOKEN_RETRY_MS = 1000L
