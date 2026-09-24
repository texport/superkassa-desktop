package kz.mybrain.superkassa.domain.workplace.usecase

import kz.mybrain.superkassa.domain.workplace.model.MapServices
import kz.mybrain.superkassa.domain.workplace.model.tidy
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceChoices

/**
 * Службы карты на этой машине.
 *
 * Пустое поле — общедоступная служба сообщества; замена — настройка,
 * а не перевыпуск приложения.
 */
class SaveMapServices(private val choices: WorkplaceChoices) {

    /** @return сохранённые адреса. */
    operator fun invoke(maps: MapServices): MapServices {
        choices.maps = maps.tidy()
        return choices.maps
    }
}
