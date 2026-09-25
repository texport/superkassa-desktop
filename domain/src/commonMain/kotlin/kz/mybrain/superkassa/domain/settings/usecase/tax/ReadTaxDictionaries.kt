package kz.mybrain.superkassa.domain.settings.usecase.tax

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.kassa.model.map
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.settings.model.TaxDictionaries

/**
 * Справочники налоговых режимов и ставок НДС — у кассы.
 *
 * Оба нужны вместе: режим без ставок и ставки без режима выбрать нельзя.
 */
class ReadTaxDictionaries(private val kassa: Kassa) {

    suspend operator fun invoke(): Answer<TaxDictionaries> = when (val regimes = kassa.ask { it.getTaxRegimes() }) {
        is Answer.Done -> kassa.ask { it.listVatRates() }.map { TaxDictionaries(regimes.value, it) }
        is Answer.Refused -> regimes
        is Answer.Failed -> regimes
    }
}
