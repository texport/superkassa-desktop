package kz.mybrain.superkassa.domain.cabinet.usecase.register

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmListParams
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa

/** Кассы этой машины — какими их знает касса в процессе приложения. */
class ReadKkmsHere(private val kassa: Kassa) {
    suspend operator fun invoke(): Answer<List<KkmResponse>> =
        when (val answer = kassa.ask { it.listKkms(KkmListParams(limit = MAX_KKMS)) }) {
            is Answer.Done -> Answer.Done(answer.value.items)
            is Answer.Refused -> answer
            is Answer.Failed -> answer
        }

    private companion object {
        /** Сколько касс этой машины читать разом: больше не бывает и у сети. */
        const val MAX_KKMS = 1000
    }
}
