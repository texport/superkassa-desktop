package kz.mybrain.superkassa.presentation.settings

import io.github.texport.superkassa.core.presentation.api.model.common.VatRateResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TaxRegimeResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kz.mybrain.superkassa.KassaDesk
import kz.mybrain.superkassa.SettingsMeasure
import kz.mybrain.superkassa.domain.settings.port.MemoryChoices
import kz.mybrain.superkassa.domain.settings.port.settingsPorts
import kz.mybrain.superkassa.domain.workplace.model.MapServices
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetSettings
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.MemoryWorkplace
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.presentation.debug.log.DebugCard
import kz.mybrain.superkassa.presentation.debug.log.LogActions
import kz.mybrain.superkassa.presentation.debug.log.LogUiState
import kz.mybrain.superkassa.presentation.kassa.sale.PanelBehaviourCard
import kz.mybrain.superkassa.presentation.print.target.PrintTargetActions
import kz.mybrain.superkassa.presentation.print.target.PrintTargetCard
import kz.mybrain.superkassa.presentation.print.target.PrintTargetUiState
import kz.mybrain.superkassa.presentation.settings.kkm.KkmSettingsUiState
import kz.mybrain.superkassa.presentation.settings.ofd.OfdSettingsUiState
import kz.mybrain.superkassa.presentation.settings.receipt.ReceiptFormUiState
import kz.mybrain.superkassa.presentation.settings.tax.TaxSettingsUiState
import kz.mybrain.superkassa.presentation.settings.workplace.WorkplaceSettingsUiState
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.update.check.UpdatesActions
import kz.mybrain.superkassa.presentation.update.check.UpdatesCard
import kz.mybrain.superkassa.presentation.update.check.UpdatesUiState

/**
 * Настройки для снимков вида: доска, собранная руками из той же кассы,
 * за которой вошёл кассир окна снимка, — те же налоги и то же оформление.
 */
internal object SettingsScene {

    /** Налоговые режимы и ставки, как их отдаёт справочник кассы. */
    val REGIMES = listOf(TaxRegimeResponse("GENERAL", words("Общеустановленный")))
    val RATES = listOf(VatRateResponse("VAT_12", 12, "НДС 12%", words("НДС 12%")))

    /** Доска настроек для окна снимка: касса, права, налоги, оформление, машина. */
    fun board(desk: KassaDesk, queued: Int? = null): SettingsBoard {
        val seat = desk.app.services.signIn.state.value
        val kkm = seat.kkm?.let { kkm -> queued?.let { kkm.copy(offlineQueueCount = it) } ?: kkm }
        return SettingsBoard(
            look = desk.look.state.value,
            lookActions = desk.look,
            kkm = KkmSettingsUiState(kkm = kkm, admin = seat.isAdmin),
            tax = TaxSettingsUiState(kkm = kkm, regimes = REGIMES, vatRates = RATES, dictionariesRead = true),
            form = ReceiptFormUiState(kkm = kkm),
            ofd = OfdSettingsUiState(kkm = kkm),
            workplace = WorkplaceSettingsUiState(kkmId = kkm?.kkmId, cabinetUrl = CabinetSettings.DEFAULT_URL),
            parts = PARTS
        )
    }

    /** Карточки других областей в состоянии по умолчанию: снимок видит их на своих местах. */
    private val PARTS = SettingsParts(
        panels = { PanelBehaviourCard(MemoryWorkplace()) },
        printTarget = { PrintTargetCard(PrintTargetUiState(kkmId = "kkm-1", printersRead = true), NO_PRINTING) },
        updates = { UpdatesCard(UpdatesUiState(), NO_UPDATES) },
        debug = { DebugCard(LogUiState(), NO_LOG) }
    )

    private val NO_PRINTING = object : PrintTargetActions {}
    private val NO_UPDATES = object : UpdatesActions {}
    private val NO_LOG = object : LogActions {}

    /**
     * Касса процесса для окна снимка: справочники налогов и адреса служб.
     *
     * Адреса кабинета, плиток и поиска — каждый свой и каждый длиннее поля:
     * одинаковые, они читались на снимке как поле, связанное не с тем значением.
     */
    fun app(desk: KassaDesk): AppContainer {
        val core = FakeCore()
        core.on("getTaxRegimes") { REGIMES }
        core.on("listVatRates") { RATES }
        core.on("getPaperWidths") { emptyList<Any>() }
        val maps = MapServices(tiles = SettingsMeasure.LONG_URL, search = SettingsMeasure.LONG_SEARCH_URL)
        val machine = MemoryChoices(cabinetUrl = SettingsMeasure.LONG_CABINET_URL, maps = maps)
        val settings = settingsPorts().copy(workplace = machine)
        return CoreScene.app(core, desk.app.services.signIn, desk.app.services.talk.notices, settings = settings)
    }

    private fun words(ru: String) = TrilingualMessageResponse(ru = ru, kk = ru, en = ru)
}
