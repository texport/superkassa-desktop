package kz.mybrain.superkassa.domain.signin.usecase

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmListParams
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.model.map
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.KkmChoice
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory

/**
 * Кассы рабочего места для входа: список, свои названия и касса прошлого раза.
 *
 * Список, который касса не отдала, не стирает прочитанного прежде:
 * о кассах тогда неизвестно ничего нового, а не «касс нет».
 */
class ReadKkmChoice(private val kassa: Kassa, private val memory: WorkplaceMemory) {

    /** @param known кассы, прочитанные прежде: остаются, если касса не ответила. */
    suspend operator fun invoke(known: List<KkmResponse>): KkmChoice {
        val answer = kassa.ask { it.listKkms(KkmListParams(limit = MAX_KKMS)) }.map { it.items }
        val kkms = (answer as? Answer.Done)?.value ?: known
        return KkmChoice(
            answer = answer,
            kkms = kkms,
            localNames = kkms.mapNotNull { kkm -> memory.localName(kkm.kkmId)?.let { kkm.kkmId to it } }.toMap(),
            rememberedId = memory.rememberedKkmId
        )
    }

    private companion object {
        /** Столько касс на одном рабочем месте не бывает; больше ядро не отдаёт за раз. */
        const val MAX_KKMS = 1000
    }
}
