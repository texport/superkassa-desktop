package kz.mybrain.superkassa.domain.cabinet.usecase.register

import io.github.texport.superkassa.core.presentation.api.model.reference.OfdEnvironmentResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa

/** Контуры БФД из справочника кассы. */
class ReadOfdEnvironments(private val kassa: Kassa) {
    suspend operator fun invoke(): Answer<List<OfdEnvironmentResponse>> = kassa.ask { it.getOfdEnvironments() }
}
