package kz.mybrain.superkassa.domain.settings.usecase

import io.github.texport.superkassa.core.presentation.api.model.reference.PaperWidthResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa

/**
 * Макеты печатной формы из справочника кассы.
 *
 * Свой список не узнал бы о новой ширине ленты, пока приложение
 * не перевыпустят.
 */
class ReadPaperWidths(private val kassa: Kassa) {

    suspend operator fun invoke(): Answer<List<PaperWidthResponse>> = kassa.ask { it.getPaperWidths() }
}
