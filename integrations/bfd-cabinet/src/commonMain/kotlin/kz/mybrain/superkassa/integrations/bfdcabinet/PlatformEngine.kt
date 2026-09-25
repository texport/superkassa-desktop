package kz.mybrain.superkassa.integrations.bfdcabinet

import io.ktor.client.engine.HttpClientEngine

/**
 * Движок Ktor этой платформы: OkHttp, Darwin или клиент JDK.
 *
 * @param pin имя сервера и его IP — когда имя в сети не находится.
 *   Соблюдает OkHttp на Android, где `/etc/hosts` не поправить; на
 *   компьютере ту же роль играет `/etc/hosts`, и замена не нужна.
 */
internal expect fun platformEngine(pin: HostPin?): HttpClientEngine

/**
 * Имя сервера и IP, по которому его искать, — строка `/etc/hosts`.
 *
 * @property host имя из адреса кабинета: с ним уходит запрос.
 * @property address IP, куда идёт соединение.
 */
data class HostPin(val host: String, val address: String)
