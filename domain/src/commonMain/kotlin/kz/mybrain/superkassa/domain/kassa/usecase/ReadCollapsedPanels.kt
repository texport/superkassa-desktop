package kz.mybrain.superkassa.domain.kassa.usecase

import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory

/** Разделы кассовой колонки, которые кассир держит свёрнутыми. */
class ReadCollapsedPanels(private val memory: WorkplaceMemory) {
    operator fun invoke(): Set<String> = memory.collapsedPanels
}
