package kz.mybrain.superkassa.domain.settings.model

import io.github.texport.superkassa.core.presentation.api.model.common.VatRateResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TaxRegimeResponse

/** Налоговые режимы и ставки НДС, как их называет справочник кассы. */
data class TaxDictionaries(val regimes: List<TaxRegimeResponse>, val vatRates: List<VatRateResponse>)
