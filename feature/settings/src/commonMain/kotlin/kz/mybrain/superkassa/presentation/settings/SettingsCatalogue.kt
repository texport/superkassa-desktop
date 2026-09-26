package kz.mybrain.superkassa.presentation.settings

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.presentation.settings.core.CoreSettingsCard
import kz.mybrain.superkassa.presentation.settings.core.KassaFactsCard
import kz.mybrain.superkassa.presentation.settings.kkm.CurrentKkmCard
import kz.mybrain.superkassa.presentation.settings.kkm.DecommissionCard
import kz.mybrain.superkassa.presentation.settings.kkm.ProgrammingCard
import kz.mybrain.superkassa.presentation.settings.ofd.DiagnosticsCard
import kz.mybrain.superkassa.presentation.settings.ofd.OfdSyncCard
import kz.mybrain.superkassa.presentation.settings.ofd.OfdTokenCard
import kz.mybrain.superkassa.presentation.settings.receipt.PrintFormCard
import kz.mybrain.superkassa.presentation.settings.tax.TaxSettingsCard
import kz.mybrain.superkassa.presentation.settings.workplace.AppearanceCard
import kz.mybrain.superkassa.presentation.settings.workplace.CabinetAddressCard
import kz.mybrain.superkassa.presentation.settings.workplace.LanguageCard
import kz.mybrain.superkassa.presentation.settings.workplace.MapServicesCard
import kz.mybrain.superkassa.presentation.settings.workplace.TradeDomainCard

/**
 * Имя настройки и раздел, в котором она стоит.
 *
 * Нужно, чтобы правило показа можно было проверить, не рисуя экран:
 * отбор возвращает имена, а не группы. Порядок здесь — порядок на экране:
 * по разделам, а внутри раздела — от повседневного к необратимому.
 */
internal enum class Setting(val section: SettingsSection) {
    CurrentKkm(SettingsSection.General),
    Programming(SettingsSection.General),
    Decommission(SettingsSection.General),
    PrintForm(SettingsSection.Printing),
    PrintTarget(SettingsSection.Printing),
    Domain(SettingsSection.Taxes),
    Tax(SettingsSection.Taxes),
    Diagnostics(SettingsSection.Bfd),
    OfdSync(SettingsSection.Bfd),
    OfdToken(SettingsSection.Bfd),
    Core(SettingsSection.Bfd),
    Appearance(SettingsSection.Look),
    Language(SettingsSection.Language),
    PanelBehaviour(SettingsSection.SalePanels),
    Debug(SettingsSection.Debug),
    Facts(SettingsSection.About),
    Updates(SettingsSection.About),
    CabinetAddress(SettingsSection.Connection),
    MapServices(SettingsSection.Connection)
}

/**
 * Настройка: своя группа и условия показа.
 *
 * @param needsRegister настройке нужна выбранная касса.
 * @param adminOnly касса отвечает по ней только администратору.
 * @param needsCabinet настройка служит кабинету: без него — на Android —
 *   ей нечего настраивать.
 * @param needsReleases настройка служит выпускам кассы: там, где приложение
 *   обновляет магазин, — на Android — ей нечего проверять.
 */
internal class SettingsCard(
    val setting: Setting,
    val needsRegister: Boolean = false,
    val adminOnly: Boolean = false,
    val needsCabinet: Boolean = false,
    val needsReleases: Boolean = false,
    val card: @Composable (SettingsBoard) -> Unit
) {
    fun visible(hasRegister: Boolean, admin: Boolean, hasCabinet: Boolean = true, hasReleases: Boolean = true) =
        (!needsRegister || hasRegister) && (!adminOnly || admin) && (!needsCabinet || hasCabinet) &&
            (!needsReleases || hasReleases)
}

/** Что видно при таком месте, таких правах, кабинете и выпусках. */
internal fun visibleSettings(
    hasRegister: Boolean,
    admin: Boolean,
    hasCabinet: Boolean = true,
    hasReleases: Boolean = true
): List<Setting> = settingsCards.filter { it.visible(hasRegister, admin, hasCabinet, hasReleases) }.map { it.setting }

/**
 * Весь список настроек приложения.
 *
 * Одно место, где видно, что у кассы вообще настраивается и что кому
 * доступно. Добавленная настройка ложится сюда со своим разделом
 * и условиями.
 */
internal val settingsCards: List<SettingsCard> = kkmCards() + appCards() + cabinetCards()

/**
 * Настройки выбранной кассы.
 *
 * Сроки обмена с БФД и доставка чека одни на все кассы этой машины, но
 * владелец ищет их там же, где связь кассы с БФД и её чеки, — и меняет их
 * только администратор, то есть уже с выбранной кассой.
 */
private fun kkmCards() = listOf(
    SettingsCard(Setting.CurrentKkm, needsRegister = true) { CurrentKkmCard(it.kkm, it.kkmActions) },
    // Режим программирования стоит сразу под кассой, которой он принадлежит:
    // вход и выход в одном месте, и состояние видно оттуда, откуда его меняют.
    SettingsCard(Setting.Programming, needsRegister = true, adminOnly = true) {
        ProgrammingCard(it.kkm, it.kkmActions)
    },
    // Снятие с учёта — последним в разделе: его не нажимают по дороге.
    SettingsCard(Setting.Decommission, needsRegister = true, adminOnly = true) {
        DecommissionCard(it.kkm, it.kkmActions)
    },
    // Печатную форму касса меняет только администратору в режиме
    // программирования; принтер на этой машине выбирает и кассир.
    SettingsCard(Setting.PrintForm, needsRegister = true, adminOnly = true) { PrintFormCard(it.form, it.formActions) },
    SettingsCard(Setting.PrintTarget, needsRegister = true) { it.parts.printTarget() },
    // Отрасль стоит перед налогами: и то и другое решает, чем наполнен
    // каждый чек этой кассы.
    SettingsCard(Setting.Domain, needsRegister = true, adminOnly = true) {
        TradeDomainCard(it.workplace, it.workplaceActions)
    },
    SettingsCard(Setting.Tax, needsRegister = true, adminOnly = true) { TaxSettingsCard(it.tax, it.taxActions) },
    SettingsCard(Setting.Diagnostics, needsRegister = true) { DiagnosticsCard(it.ofd, it.ofdActions) },
    SettingsCard(Setting.OfdSync, needsRegister = true, adminOnly = true) { OfdSyncCard(it.ofd, it.ofdActions) },
    SettingsCard(Setting.OfdToken, needsRegister = true, adminOnly = true) { OfdTokenCard(it.ofd, it.ofdActions) },
    SettingsCard(Setting.Core, adminOnly = true) { CoreSettingsCard(it.core, it.coreActions) },
)

/**
 * Настройки приложения на этой машине: их задают и до входа.
 *
 * Сведения о программе и кассовом ядре видны всем и без входа — поддержке
 * они нужны, когда касса не открылась. Выпуски — там, где касса обновляет
 * себя сама. Отладка условий не имеет: она нужна ровно тогда, когда войти
 * нельзя.
 */
private fun appCards() = listOf(
    SettingsCard(Setting.Appearance) { AppearanceCard(it.look, it.lookActions) },
    SettingsCard(Setting.Language) { LanguageCard(it.look, it.lookActions) },
    SettingsCard(Setting.PanelBehaviour) { it.parts.panels() },
    SettingsCard(Setting.Debug) { it.parts.debug() },
    SettingsCard(Setting.Facts) { KassaFactsCard(it.core) },
    SettingsCard(Setting.Updates, needsReleases = true) { it.parts.updates() }
)

/** Подключение к кабинету БФД и его карте — только там, где кабинет собран. */
private fun cabinetCards() = listOf(
    SettingsCard(Setting.CabinetAddress, needsCabinet = true) { CabinetAddressCard(it.workplace, it.workplaceActions) },
    SettingsCard(Setting.MapServices, needsCabinet = true) { MapServicesCard(it.workplace, it.workplaceActions) }
)
