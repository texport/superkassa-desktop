package kz.mybrain.superkassa.domain.workplace.usecase

import kz.mybrain.superkassa.domain.workplace.port.WorkplaceChoices

/**
 * Вид отрасли кассы на этой машине.
 *
 * Своей настройки отрасли у кассы нет — вид отрасли уходит в неё с каждым
 * чеком, — поэтому помнит его рабочее место, за каждой кассой свой.
 * Выбор действует сразу: касса кланяется ему на следующем же чеке.
 */
class ChooseTradeDomain(private val choices: WorkplaceChoices) {

    /** @param code вид отрасли, как его называет ядро; `null` — торговля. */
    operator fun invoke(kkmId: String, code: String?) = choices.chooseDomain(kkmId, code)
}
