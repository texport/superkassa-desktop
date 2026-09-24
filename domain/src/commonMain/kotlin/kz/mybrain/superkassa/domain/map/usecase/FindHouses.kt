package kz.mybrain.superkassa.domain.map.usecase

import kz.mybrain.superkassa.domain.map.model.MapPlace
import kz.mybrain.superkassa.domain.map.port.Maps

/**
 * Дома торговых точек по их адресам — по одному.
 *
 * Кабинет при источнике «адрес торговой точки» координат не даёт: адресный
 * регистр их не хранит. Дом ищет служба карт, и паузу между вопросами
 * держит она сама; найденное она же помнит.
 *
 * Найденный прежде адрес не спрашивается снова, ненайденный — спрашивается:
 * не ответившая служба ставит кассу в «не нашли» только до следующего
 * обновления.
 */
class FindHouses(private val maps: Maps) {

    /**
     * @param addresses адреса точек, дома которых нужны.
     * @param found адреса, дом которых уже найден.
     * @param onAnswer ответ по адресу, как только он пришёл; `null` — дом не найден
     *   или служба не ответила.
     */
    suspend operator fun invoke(addresses: List<String>, found: Set<String>, onAnswer: (String, MapPlace?) -> Unit) {
        addresses.filterNot { it in found }.forEach { address ->
            onAnswer(address, maps.find(address)?.firstOrNull())
        }
    }
}
