package kz.mybrain.superkassa.domain.kassa.usecase

import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory

/** Запоминает свёрнутые разделы кассовой колонки между запусками. */
class RememberCollapsedPanels(private val memory: WorkplaceMemory) {
    operator fun invoke(names: Set<String>) {
        memory.collapsedPanels = names
    }
}
