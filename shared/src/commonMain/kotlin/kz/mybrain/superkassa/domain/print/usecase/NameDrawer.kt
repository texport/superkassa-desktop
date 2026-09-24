package kz.mybrain.superkassa.domain.print.usecase

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.kkm.model.displayName
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory

/** Название кассы, чей пин спрашивается: пин у касс разный, и владелец должен видеть, к какой. */
class NameDrawer(private val memory: WorkplaceMemory) {

    operator fun invoke(kkm: KkmResponse): String = kkm.displayName(memory.localName(kkm.kkmId))
}
