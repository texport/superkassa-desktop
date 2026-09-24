package kz.mybrain.superkassa.domain.setup.usecase

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmListParams
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.OfdEnvironmentResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa

/**
 * Контуры БФД и кассы этого рабочего места.
 *
 * По кассам подставляется контур и видно, пройден ли последний шаг.
 * Касса, промолчавшая о том или другом, прежнего ответа не отменяет:
 * на его месте `null`.
 */
class ReadSetupContext(private val kassa: Kassa) {

    suspend operator fun invoke(): Pair<List<OfdEnvironmentResponse>?, List<KkmResponse>?> {
        val contours = (kassa.ask { it.getOfdEnvironments() } as? Answer.Done)?.value
        val kkms = (kassa.ask { it.listKkms(KkmListParams(limit = MAX_KKMS)) } as? Answer.Done)?.value?.items
        return contours to kkms
    }

    private companion object {
        /** Столько касс на одном рабочем месте не бывает; больше касса не отдаёт за раз. */
        const val MAX_KKMS = 1000
    }
}
