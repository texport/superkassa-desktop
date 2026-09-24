package kz.mybrain.superkassa.domain.cabinet.usecase.places

import kz.mybrain.superkassa.domain.cabinet.model.AddressLevel
import kz.mybrain.superkassa.domain.cabinet.model.AddressSuggestion
import kz.mybrain.superkassa.domain.cabinet.model.distinctSuggestions
import kz.mybrain.superkassa.domain.cabinet.port.CabinetAddresses

/**
 * Подсказки адресного регистра на шаге [level] под выбранным [parent].
 *
 * Свободного поиска по адресу целиком регистр не даёт: регион ищется сам
 * по себе, пункт — под регионом или пунктом, улица — под пунктом, дом —
 * под улицей. Полные двойники из подсказок убираются — см. [distinctSuggestions].
 */
class LookUpAddress(private val addresses: CabinetAddresses) {
    suspend operator fun invoke(level: AddressLevel, parent: Long, needle: String): List<AddressSuggestion> {
        val found = when (level) {
            AddressLevel.Region -> addresses.regions(needle)
            AddressLevel.Locality -> addresses.localities(parent, needle)
            AddressLevel.Street -> addresses.streets(parent, needle)
            AddressLevel.Building -> addresses.buildings(parent, needle)
        }
        return distinctSuggestions(found)
    }
}
