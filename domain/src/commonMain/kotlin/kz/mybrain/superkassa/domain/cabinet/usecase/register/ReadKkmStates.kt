package kz.mybrain.superkassa.domain.cabinet.usecase.register

import io.github.texport.superkassa.core.presentation.api.model.reference.KkmStateResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa

/** Названия состояний кассы на трёх языках — из справочника кассы. */
class ReadKkmStates(private val kassa: Kassa) {
    suspend operator fun invoke(): Answer<List<KkmStateResponse>> = kassa.ask { it.getKkmStates() }
}
