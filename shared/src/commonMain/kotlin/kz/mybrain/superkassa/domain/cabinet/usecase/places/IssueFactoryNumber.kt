package kz.mybrain.superkassa.domain.cabinet.usecase.places

import io.github.texport.superkassa.core.presentation.api.model.common.FactoryNumberResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.port.Kassa

/**
 * Заводской номер и год для своей кассы: его присваивает касса приложения,
 * в кабинете его не набирают, а получают.
 */
class IssueFactoryNumber(private val kassa: Kassa) {
    suspend operator fun invoke(): Answer<FactoryNumberResponse> = kassa.ask { it.generateFactoryInfo() }
}
