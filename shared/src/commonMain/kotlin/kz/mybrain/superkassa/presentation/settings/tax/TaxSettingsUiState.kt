package kz.mybrain.superkassa.presentation.settings.tax

import io.github.texport.superkassa.core.presentation.api.model.common.VatRateResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TaxRegimeResponse
import kz.mybrain.superkassa.domain.settings.model.KkmNeed
import kz.mybrain.superkassa.domain.settings.model.KkmSettingRules
import kz.mybrain.superkassa.presentation.settings.KkmDrafts

/**
 * Налоги кассы, автозакрытие и автоизъятие.
 *
 * @property regimes налоговые режимы из справочника кассы.
 * @property vatRates ставки НДС из справочника кассы.
 * @property dictionariesRead справочники прочитаны; пустые непрочитанные
 *   и пустые прочитанные — разные беды.
 * @property drafts выбранные, но не сохранённые режим и ставка — у каждой
 *   кассы свои: уход к другой кассе их не стирает.
 */
data class TaxSettingsUiState(
    val kkm: KkmResponse? = null,
    val regimes: List<TaxRegimeResponse> = emptyList(),
    val vatRates: List<VatRateResponse> = emptyList(),
    val dictionariesRead: Boolean = false,
    val drafts: KkmDrafts<TaxDraft> = KkmDrafts(),
    val busy: Boolean = false
) {
    /** Выбранный, но не сохранённый режим этой кассы. */
    val regimeDraft: String? get() = drafts.of(kkm?.kkmId)?.regime

    /** Выбранная, но не сохранённая ставка этой кассы. */
    val vatDraft: String? get() = drafts.of(kkm?.kkmId)?.vat

    /** Выбор этой кассы с правкой [change]. */
    fun drafted(change: (TaxDraft) -> TaxDraft): TaxSettingsUiState =
        copy(drafts = drafts.with(kkm?.kkmId, change(drafts.of(kkm?.kkmId) ?: TaxDraft())))

    /** Выбор этой кассы сохранён: черновик забыт. */
    fun saved(): TaxSettingsUiState = copy(drafts = drafts.with(kkm?.kkmId, null))

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

/** Выбранные, но не сохранённые режим и ставка одной кассы. */
data class TaxDraft(val regime: String? = null, val vat: String? = null)

/** Что владелец меняет в налогах кассы. Пустые действия — для снимков вида. */
interface TaxSettingsActions {
    fun chooseRegime(code: String) = Unit

    fun chooseVat(code: String) = Unit

    fun saveTax() = Unit

    fun retryDictionaries() = Unit

    fun switchAutoClose(on: Boolean) = Unit

    fun switchAutoCashout(on: Boolean) = Unit
}
