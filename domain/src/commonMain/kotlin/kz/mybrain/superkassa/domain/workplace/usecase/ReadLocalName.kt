package kz.mybrain.superkassa.domain.workplace.usecase

import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory

/**
 * Имя кассы этой машины, данное ей на рабочем месте: так её и показывают.
 *
 * Без `suspend`: имя лежит в настройках рабочего места и читается сразу,
 * а показывает его разметка карточки.
 */
class ReadLocalName(private val memory: WorkplaceMemory) {
    operator fun invoke(kkmId: String): String? = memory.localName(kkmId)
}
