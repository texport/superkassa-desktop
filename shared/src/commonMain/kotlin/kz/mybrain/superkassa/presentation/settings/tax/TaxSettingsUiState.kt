package kz.mybrain.superkassa.presentation.settings.tax

import io.github.texport.superkassa.core.presentation.api.model.common.VatRateResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TaxRegimeResponse
import kz.mybrain.superkassa.domain.settings.model.KkmNeed
import kz.mybrain.superkassa.domain.settings.model.KkmSettingRules

/**
 * Налоги кассы, автозакрытие и автоизъятие.
 *
 * @property regimes налоговые режимы из справочника кассы.
 * @property vatRates ставки НДС из справочника кассы.
 * @property dictionariesRead справочники прочитаны; пустые непрочитанные
 *   и пустые прочитанные — разные беды.
 * @property regimeDraft выбранный, но не сохранённый режим.
 * @property vatDraft выбранная, но не сохранённая ставка.
 */
data class TaxSettingsUiState(
    val kkm: KkmResponse? = null,
    val regimes: List<TaxRegimeResponse> = emptyList(),
    val vatRates: List<VatRateResponse> = emptyList(),
    val dictionariesRead: Boolean = false,
    val regimeDraft: String? = null,
    val vatDraft: String? = null,
    val busy: Boolean = false
) {
    /** Режим в поле: выбранный, иначе тот, что у кассы. */
    val regime: String? get() = regimeDraft ?: kkm?.taxRegime?.takeIf { it.isNotBlank() }

    /** Ставка в поле: выбранная, иначе та, что у кассы. */
    val vatGroup: String? get() = vatDraft ?: kkm?.defaultVatGroup?.takeIf { it.isNotBlank() }

    /**
     * Ставка по умолчанию следует за режимом: у неплательщика НДС выбирать
     * не из чего, и пара «без НДС» + «НДС 12%» упиралась в отказ кассы
     * на первом же чеке.
     */
    val vatChoosable: Boolean get() = KkmSettingRules.vatChoosable(regime)

    /** Выбирать не из чего: справочник не прочитан или пуст. */
    val dictionariesMissing: Boolean get() = regimes.isEmpty() || vatRates.isEmpty()

    /** Чего касса требует для смены налогов и что из этого выполнено. */
    val needs: List<KkmNeed> get() = kkm?.let(KkmSettingRules::tax).orEmpty()

    /** Сохранять есть что, и касса это примет. */
    val savable: Boolean
        get() = regime != null && vatGroup != null && KkmSettingRules.met(needs) && !busy &&
            (regime != kkm?.taxRegime || vatGroup != kkm?.defaultVatGroup)

    /** Автозакрытие и автоизъятие касса меняет только в режиме программирования. */
    val switchable: Boolean get() = kkm?.let { KkmSettingRules.met(KkmSettingRules.branding(it)) } == true && !busy
}

/** Что владелец меняет в налогах кассы. Пустые действия — для снимков вида. */
interface TaxSettingsActions {
    fun chooseRegime(code: String) = Unit

    fun chooseVat(code: String) = Unit

    fun saveTax() = Unit

    fun retryDictionaries() = Unit

    fun switchAutoClose(on: Boolean) = Unit

    fun switchAutoCashout(on: Boolean) = Unit
}
