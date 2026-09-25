package kz.mybrain.superkassa.presentation.settings.preview

import io.github.texport.superkassa.core.domain.api.model.settings.CoreMode
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.api.model.settings.StorageSettings
import io.github.texport.superkassa.core.presentation.api.model.common.VatRateResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.OfdServiceInfoResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TaxRegimeResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kz.mybrain.superkassa.domain.settings.model.DeliveryChannel
import kz.mybrain.superkassa.domain.settings.model.DeliveryField
import kz.mybrain.superkassa.domain.settings.model.KassaFacts
import kz.mybrain.superkassa.domain.settings.model.OfdSummary
import kz.mybrain.superkassa.domain.workplace.model.MapServices
import kz.mybrain.superkassa.presentation.settings.SettingsBoard
import kz.mybrain.superkassa.presentation.settings.core.CoreSettingsUiState
import kz.mybrain.superkassa.presentation.settings.core.DeliveryUiState
import kz.mybrain.superkassa.presentation.settings.kkm.KkmSettingsUiState
import kz.mybrain.superkassa.presentation.settings.ofd.OfdSettingsUiState
import kz.mybrain.superkassa.presentation.settings.receipt.ReceiptFormUiState
import kz.mybrain.superkassa.presentation.settings.tax.TaxSettingsUiState
import kz.mybrain.superkassa.presentation.settings.workplace.WorkplaceSettingsUiState

/**
 * Подставные состояния настроек для превью: касса, права, налоги, машина.
 *
 * Только состояния — без моделей, кассы и сети: превью рисует то же, что
 * экран получил бы от моделей, и ни с чем не говорит. Касса — в режиме
 * программирования, чтобы все поля стояли живыми.
 */
internal object SettingsSamples {

    /** Касса у входа: заведена, в режиме программирования, общий режим и НДС 12 %. */
    val kkm = KkmResponse(
        kkmId = "kkm-1",
        createdAt = 0,
        updatedAt = 0,
        mode = "REGISTRATION",
        state = "PROGRAMMING",
        name = "Касса у входа",
        kkmKgdId = "000000200042",
        factoryNumber = "SK-000042",
        ofdServiceInfo = OfdServiceInfoResponse(
            orgTitle = "ТОО «Пример»",
            orgAddress = "Алматы, Абая 150",
            orgAddressKz = "Алматы, Абай 150",
            orgIinOrBin = "000000000000",
            orgOked = "47111",
            geoLatitude = 0,
            geoLongitude = 0,
            geoSource = "MANUAL"
        ),
        taxRegime = GENERAL,
        defaultVatGroup = VAT,
        autoCloseShift = true,
        isProgrammingMode = true
    )

    /** Та же касса в обычной работе: режим программирования выключен. */
    val working = kkm.copy(state = "ACTIVE", isProgrammingMode = false)

    private val regimes = listOf(TaxRegimeResponse(GENERAL, words("Общеустановленный")))
    private val rates = listOf(VatRateResponse(VAT, VAT_PERCENT, "НДС 12%", words("НДС 12%")))

    /** Настройки кассы на машине: рабочее место, база на диске, правка открыта. */
    private val core = CoreSettings(
        CoreMode.DESKTOP,
        StorageSettings("SQLITE", "jdbc:sqlite:superkassa.db"),
        allowChanges = true
    )

    val coreState = CoreSettingsUiState(
        settings = core,
        about = KassaFacts(appVersion = "1.0.6", coreVersion = "1.5.0", directory = "~/.superkassa", kkmCount = 2)
    )

    val delivery = DeliveryUiState.of(core).copy(
        saved = mapOf(DeliveryField.TelegramToken to "***"),
        enabledSaved = setOf(DeliveryChannel.Telegram),
        configured = setOf(DeliveryChannel.Telegram)
    )

    val ofd = OfdSettingsUiState(
        kkm = kkm,
        linkAlive = true,
        admin = true,
        summary = OfdSummary(
            answer = "OK",
            organization = "ТОО «Пример»",
            bin = "000000000000",
            kgdNumber = "000000200042"
        )
    )

    /** Доска администратора за кассой [kkm]. */
    fun admin(kkm: KkmResponse = this.kkm) = SettingsBoard(
        kkm = KkmSettingsUiState(kkm = kkm, admin = true),
        tax = TaxSettingsUiState(kkm = kkm, regimes = regimes, vatRates = rates, dictionariesRead = true),
        form = ReceiptFormUiState(kkm = kkm),
        ofd = ofd.copy(kkm = kkm),
        core = coreState,
        delivery = delivery,
        workplace = WorkplaceSettingsUiState(
            kkmId = kkm.kkmId,
            cabinetUrl = "https://cabinet.example.kz",
            publicMaps = MapServices(tiles = "https://tile.openstreetmap.org/{z}/{x}/{y}.png")
        )
    )

    /** Доска кассира: служебного не видно. */
    fun cashier() = admin(working).let { it.copy(kkm = it.kkm.copy(admin = false), ofd = it.ofd.copy(admin = false)) }

    /** Доска до входа: кассы нет, прав нет. */
    fun door() = SettingsBoard(core = coreState)

    private fun words(ru: String) = TrilingualMessageResponse(ru = ru, kk = ru, en = ru)

    private const val GENERAL = "GENERAL"
    private const val VAT = "VAT_12"
    private const val VAT_PERCENT = 12
}
