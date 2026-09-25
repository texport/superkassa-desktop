package kz.mybrain.superkassa.domain.workplace.usecase

import kz.mybrain.superkassa.domain.workplace.port.WorkplaceChoices

/**
 * IP сервера кабинета БФД на этой машине — когда имя кабинета в сети
 * не находится: VPN без своего DNS, планшет без `/etc/hosts`.
 *
 * Кабинет открывается по этому IP, но под своим именем: на сервере рядом
 * с ним другие службы, и по голому IP сервер отвечал «страница не
 * найдена». Пустое поле снимает замену — имя снова находит сеть. Новый
 * IP берётся при следующем входе в кабинет.
 */
class SaveCabinetServer(private val choices: WorkplaceChoices) {

    /** @return сохранённый IP; `null` — набранное не отличается от записанного. */
    operator fun invoke(typed: String): String? {
        val tidy = typed.trim()
        if (tidy == choices.cabinetServer) return null
        choices.cabinetServer = tidy
        return tidy
    }
}
