package kz.mybrain.superkassa.desktop.app

import kz.mybrain.superkassa.desktop.server.cabinet.DebugIdentity
import kz.mybrain.superkassa.desktop.server.cabinet.me

/**
 * Вход в кабинет без ЭЦП — режим разработки на период MVP.
 *
 * Отдельным файлом, потому что это не часть работы кабинета, а временная
 * подпорка: когда кабинет начнёт проверять подпись при входе, файл уходит
 * целиком, не оставляя следов в сеансе.
 *
 * Кабинет берёт владельца и компанию из заголовков запроса и не проверяет
 * подпись при входе. Доступа он при этом не выдаёт, поэтому доступом здесь
 * служит сама личность: пока она задана, клиент подписывает ею каждый запрос.
 * Заявления в таком сеансе всё равно подписываются настоящей ЭЦП, и кабинет
 * сверяет ИИН сертификата с ИИН сеанса — вводить надо свой.
 */
internal suspend fun CabinetSession.enterAsDeveloper(iin: String, bin: String): Boolean {
    val identity = DebugIdentity(iin.trim(), bin.trim())
    client.debugIdentity = identity
    val entered = guard { client.me(CabinetSession.DEVELOPER_ACCESS) }
    if (entered == null) {
        client.debugIdentity = null
        return false
    }
    access.enter(CabinetSession.DEVELOPER_ACCESS, entered)
    return true
}
