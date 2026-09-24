package kz.mybrain.superkassa.domain.workplace.usecase

import kz.mybrain.superkassa.domain.workplace.model.ServiceAddress
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceChoices

/**
 * Адрес кабинета БФД на этой машине.
 *
 * Негодный адрес не сохраняется: по адресу без схемы входа в кабинет
 * не будет вовсе. Новый адрес берётся при следующем входе в кабинет,
 * а не под открытым сеансом.
 */
class SaveCabinetAddress(private val choices: WorkplaceChoices) {

    /** @return сохранённый адрес; `null` — набранное негодно или не отличается от записанного. */
    operator fun invoke(typed: String): String? {
        if (!ServiceAddress.changed(typed, choices.cabinetUrl)) return null
        choices.cabinetUrl = ServiceAddress.tidy(typed)
        return choices.cabinetUrl
    }
}
