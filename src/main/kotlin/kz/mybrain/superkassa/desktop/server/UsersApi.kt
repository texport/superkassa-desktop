package kz.mybrain.superkassa.desktop.server

import io.ktor.http.HttpMethod

/**
 * Кассиры и режим программирования.
 */
suspend fun ServerClient.users(kkmId: String, pin: String): List<KkmUser> =
    request(HttpMethod.Get, "/kkm/$kkmId/users", pin = pin)

suspend fun ServerClient.addUser(kkmId: String, body: KkmUserRequest, pin: String): KkmUser =
    request(HttpMethod.Post, "/kkm/$kkmId/users", body, pin)

suspend fun ServerClient.changeUserPin(kkmId: String, userId: String, newPin: String, pin: String) {
    call(HttpMethod.Put, "/kkm/$kkmId/users/$userId", mapOf("userPin" to newPin), pin)
        .let { if (!it.status.isSuccessful()) throw refusalOf(it) }
}

suspend fun ServerClient.removeUser(kkmId: String, userId: String, pin: String) {
    call(HttpMethod.Delete, "/kkm/$kkmId/users/$userId", null, pin)
        .let { if (!it.status.isSuccessful()) throw refusalOf(it) }
}

suspend fun ServerClient.enterProgramming(kkmId: String, pin: String) {
    call(HttpMethod.Post, "/kkm/$kkmId/programming/enter", null, pin)
        .let { if (!it.status.isSuccessful()) throw refusalOf(it) }
}

suspend fun ServerClient.exitProgramming(kkmId: String, pin: String) {
    call(HttpMethod.Post, "/kkm/$kkmId/programming/exit", null, pin)
        .let { if (!it.status.isSuccessful()) throw refusalOf(it) }
}

/** Связь с ОФД: жив ли канал прямо сейчас. */
suspend fun ServerClient.pingOfd(kkmId: String, pin: String): Boolean =
    call(HttpMethod.Get, "/kkm/$kkmId/ofd/ping", null, pin).status.isSuccessful()

private fun io.ktor.http.HttpStatusCode.isSuccessful(): Boolean = value in 200..299
