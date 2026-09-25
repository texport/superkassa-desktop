package kz.mybrain.superkassa.presentation.settings

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.domain.settings.port.SettingsPorts
import kz.mybrain.superkassa.presentation.common.look.LookActions
import kz.mybrain.superkassa.presentation.common.look.LookUiState
import kz.mybrain.superkassa.presentation.common.look.LookViewModel
import kz.mybrain.superkassa.presentation.common.model.WindowServices
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.settings.core.CoreSettingsActions
import kz.mybrain.superkassa.presentation.settings.core.CoreSettingsUiState
import kz.mybrain.superkassa.presentation.settings.core.DeliveryActions
import kz.mybrain.superkassa.presentation.settings.core.DeliveryUiState
import kz.mybrain.superkassa.presentation.settings.core.coreSettingsViewModel
import kz.mybrain.superkassa.presentation.settings.core.deliveryViewModel
import kz.mybrain.superkassa.presentation.settings.kkm.KkmSettingsActions
import kz.mybrain.superkassa.presentation.settings.kkm.KkmSettingsUiState
import kz.mybrain.superkassa.presentation.settings.kkm.kkmSettingsViewModel
import kz.mybrain.superkassa.presentation.settings.ofd.OfdSettingsActions
import kz.mybrain.superkassa.presentation.settings.ofd.OfdSettingsUiState
import kz.mybrain.superkassa.presentation.settings.ofd.ofdSettingsViewModel
import kz.mybrain.superkassa.presentation.settings.receipt.ReceiptFormActions
import kz.mybrain.superkassa.presentation.settings.receipt.ReceiptFormUiState
import kz.mybrain.superkassa.presentation.settings.receipt.receiptFormViewModel
import kz.mybrain.superkassa.presentation.settings.tax.TaxSettingsActions
import kz.mybrain.superkassa.presentation.settings.tax.TaxSettingsUiState
import kz.mybrain.superkassa.presentation.settings.tax.taxSettingsViewModel
import kz.mybrain.superkassa.presentation.settings.workplace.WorkplaceSettingsActions
import kz.mybrain.superkassa.presentation.settings.workplace.WorkplaceSettingsUiState
import kz.mybrain.superkassa.presentation.settings.workplace.workplaceSettingsViewModel

/**
 * Из чего рисуются карточки настроек: состояния моделей и их действия.
 *
 * Карточка берёт отсюда своё и ничего не спрашивает сама: запросы
 * и правила живут в моделях, а здесь — их итог на эту минуту.
 *
 * Доска — только состояние и действия: превью и снимки вида собирают
 * её руками, без моделей, кассы и сети.
 *
 * @property look вид окна: тема, тон, шрифт, язык — один на все разделы.
 * @property parts карточки других областей — принтер кассы, обновления,
 *   журнал: каждая со своей моделью, настройки их только расставляют.
 */
data class SettingsBoard(
    val look: LookUiState = LookUiState(),
    val lookActions: LookActions = object : LookActions {},
    val kkm: KkmSettingsUiState = KkmSettingsUiState(),
    val kkmActions: KkmSettingsActions = object : KkmSettingsActions {},
    val tax: TaxSettingsUiState = TaxSettingsUiState(),
    val taxActions: TaxSettingsActions = object : TaxSettingsActions {},
    val form: ReceiptFormUiState = ReceiptFormUiState(),
    val formActions: ReceiptFormActions = object : ReceiptFormActions {},
    val ofd: OfdSettingsUiState = OfdSettingsUiState(),
    val ofdActions: OfdSettingsActions = object : OfdSettingsActions {},
    val core: CoreSettingsUiState = CoreSettingsUiState(),
    val coreActions: CoreSettingsActions = object : CoreSettingsActions {},
    val delivery: DeliveryUiState = DeliveryUiState(),
    val deliveryActions: DeliveryActions = object : DeliveryActions {},
    val workplace: WorkplaceSettingsUiState = WorkplaceSettingsUiState(),
    val workplaceActions: WorkplaceSettingsActions = object : WorkplaceSettingsActions {},
    val parts: SettingsParts = SettingsParts()
)

/**
 * Карточки других областей, стоящие среди настроек.
 *
 * Разделы колонки продажи — область кассы, принтер кассы — печати, выпуски —
 * обновлений, журнал — отладки:
 * их модели и карточки живут там, а настройки получают готовую карточку
 * от каркаса окна и только ставят её на своё место. Без карточки место
 * пустое — так рисуются снимки вида, которым модели не нужны.
 *
 * @property hasCabinet собран ли на платформе кабинет — это знает каркас
 *   окна: без кабинета адреса кабинета и служб карты настраивать незачем.
 * @property hasReleases проверяет ли касса свои выпуски сама: на Android
 *   приложение обновляет магазин, и карточки выпусков там нет.
 */
class SettingsParts(
    val panels: @Composable () -> Unit = {},
    val printTarget: @Composable () -> Unit = {},
    val updates: @Composable () -> Unit = {},
    val debug: @Composable () -> Unit = {},
    val hasCabinet: Boolean = true,
    val hasReleases: Boolean = true
)

/**
 * Собирает доску настроек из моделей окна.
 *
 * @param look вид окна.
 * @param parts карточки других областей.
 */
@Composable
fun settingsBoard(
    services: WindowServices,
    ports: SettingsPorts,
    look: LookViewModel,
    parts: SettingsParts
): SettingsBoard {
    val kkm = kkmSettingsViewModel(services, ports)
    val tax = taxSettingsViewModel(services)
    val form = receiptFormViewModel(services)
    val ofd = ofdSettingsViewModel(services)
    return SettingsBoard(
        look = look.state.collectAsScreenState().value,
        lookActions = look,
        kkm = kkm.state.collectAsScreenState().value,
        kkmActions = kkm,
        tax = tax.state.collectAsScreenState().value,
        taxActions = tax,
        form = form.state.collectAsScreenState().value,
        formActions = form,
        ofd = ofd.state.collectAsScreenState().value,
        ofdActions = ofd,
        parts = parts
    ).withMachine(services, ports)
}

/** Настройки самой машины и кассы на ней: рабочее место, сроки обмена с БФД и доставка чека. */
@Composable
private fun SettingsBoard.withMachine(services: WindowServices, ports: SettingsPorts): SettingsBoard {
    val workplace = workplaceSettingsViewModel(services, ports)
    val core = coreSettingsViewModel(services, ports)
    val delivery = deliveryViewModel(services, ports)
    return copy(
        workplace = workplace.state.collectAsScreenState().value,
        workplaceActions = workplace,
        core = core.state.collectAsScreenState().value,
        coreActions = core,
        delivery = delivery.state.collectAsScreenState().value,
        deliveryActions = delivery
    )
}
