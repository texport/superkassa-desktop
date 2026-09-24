package kz.mybrain.superkassa.domain.document.usecase

import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa

/**
 * Названия видов документа со слов кассы: ключ — код вида.
 *
 * Справочник — не беда, если он не прочитан: вид документа тогда назван
 * своими словами, и экран показывается всё равно. Поэтому отказ здесь
 * не объявляется, а даёт `null` — и прочитанное прежде остаётся на месте.
 */
class ReadDocumentTypes(private val kassa: Kassa) {
    suspend operator fun invoke(): Map<String, TrilingualMessageResponse>? =
        (kassa.ask { it.getDocumentTypes() } as? Answer.Done)?.value?.associate { it.code to it.name }
}
