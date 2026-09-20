package kz.mybrain.superkassa.desktop.ui.analytics

import kz.mybrain.superkassa.desktop.server.cabinet.ExchangeAddress

/**
 * Отбор и поиск по адресам обмена.
 *
 * Список приходит по убыванию последнего обмена, и порядок этот
 * сохраняется: свежая связь важнее прочего. Отбор по кассе и поиск
 * работают вместе — владелец сначала выбирает кассу, потом ищет среди
 * её адресов.
 *
 * Поиск идёт по всему, что видно в строке: сам адрес, номер КГД, своё
 * название кассы, номер машины и торговая точка. Искать только по
 * адресу мало: владелец помнит кассу по названию, а не по адресу,
 * с которого она вышла на связь.
 */
fun exchangeRows(addresses: List<ExchangeAddress>, query: String, register: String?): List<ExchangeAddress> {
    val needle = query.trim().lowercase()
    return addresses
        .filter { register == null || it.cashRegisterId == register }
        .filter { needle.isBlank() || it.matches(needle) }
}

/** Совпадает ли строка с искомым. */
private fun ExchangeAddress.matches(needle: String): Boolean =
    searchable().any { it.lowercase().contains(needle) }

/** Всё, что видно в строке и по чему поэтому ищут. */
private fun ExchangeAddress.searchable(): List<String> = listOfNotNull(
    address,
    registrationNumber,
    internalName,
    retailPlaceName,
    kkmId.toString()
)

/** Кассы, встречающиеся в списке: из них собирается отбор. */
fun exchangeRegisters(addresses: List<ExchangeAddress>): List<ExchangeAddress> =
    addresses.distinctBy { it.cashRegisterId }
