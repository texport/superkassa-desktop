package kz.mybrain.superkassa.presentation.settings

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.presentation.settings.core.CoreSettingsCard
import kz.mybrain.superkassa.presentation.settings.core.DeliveryCard
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
import kz.mybrain.superkassa.presentation.settings.workplace.MapServicesCard
import kz.mybrain.superkassa.presentation.settings.workplace.TradeDomainCard

/**
 * Имя настройки.
 *
 * Нужно, чтобы правило показа можно было проверить, не рисуя экран:
 * отбор возвращает имена, а не карточки. Порядок здесь — порядок
 * на экране.
 */
internal enum class Setting {
    Appearance, PanelBehaviour,
    CabinetAddress, MapServices,
    Facts, Core, Delivery, Updates, Debug,
    CurrentKkm, Programming, PrintForm, PrintTarget,
    Domain,
    Tax, OfdSync, OfdToken, Diagnostics, Decommission
}

/**
 * Настройка: своя карточка и условия показа.
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
    val group: SettingsGroup,
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
 * доступно. Добавленная карточка ложится сюда со своим хозяйством
 * и условиями, а не в один из двух экранов, как было раньше.
 *
 * Порядок внутри хозяйства идёт от повседневного к необратимому: снятие
 * с учёта последним, чтобы его не нажимали по дороге к диагностике.
 */
internal val settingsCards = listOf(
    SettingsCard(Setting.Appearance, SettingsGroup.Look) { AppearanceCard(it.look) },
    SettingsCard(Setting.PanelBehaviour, SettingsGroup.Look) { it.parts.panels() },

    // Адреса служб задают раньше, чем куда-либо входят: без адреса кабинета
    // кассу не завести. Кабинет стоит рядом с картой, а не своей вкладкой:
    // его адрес тоже хранится на этой машине и ищется там же, где остальные.
    // Карта нужна только разделам кабинета: без кабинета не видно обеих.
    SettingsCard(Setting.CabinetAddress, SettingsGroup.Addresses, needsCabinet = true) {
        CabinetAddressCard(it.workplace, it.workplaceActions)
    },
    SettingsCard(Setting.MapServices, SettingsGroup.Addresses, needsCabinet = true) {
        MapServicesCard(it.workplace, it.workplaceActions)
    },

    // Сведения о кассе — версии, режим, протокол, хранилище — видны всем
    // и до входа: поддержке они нужны, когда касса не открылась или не пускает.
    SettingsCard(Setting.Facts, SettingsGroup.Program) { KassaFactsCard(it.core) },
    // Настройки самой кассы на этой машине — сроки обмена с БФД и доставка —
    // одни на все её кассы и меняются только администратором.
    SettingsCard(Setting.Core, SettingsGroup.Exchange, adminOnly = true) { CoreSettingsCard(it.core, it.coreActions) },
    SettingsCard(Setting.Delivery, SettingsGroup.Exchange, adminOnly = true) {
        DeliveryCard(it.delivery, it.deliveryActions)
    },
    // Версия кассы и выпуски не зависят ни от кассы, ни от прав: узнать,
    // что стоит и что вышло, можно с экрана входа. На Android приложение
    // обновляет магазин: проверять там нечего, а версия стоит в сведениях.
    SettingsCard(Setting.Updates, SettingsGroup.Program, needsReleases = true) { it.parts.updates() },
    // Отладка нужна ровно тогда, когда войти нельзя: касса не открылась,
    // список касс пуст. Условий у неё нет намеренно, а место — за тем,
    // что кассир читает каждый день.
    SettingsCard(Setting.Debug, SettingsGroup.Program) { it.parts.debug() },

    SettingsCard(Setting.CurrentKkm, SettingsGroup.Current, needsRegister = true) {
        CurrentKkmCard(it.kkm, it.kkmActions)
    },
    // Режим программирования стоит сразу под кассой, которой он
    // принадлежит: вход и выход в одном месте, и состояние видно оттуда,
    // откуда его меняют.
    SettingsCard(
        Setting.Programming,
        SettingsGroup.Current,
        needsRegister = true,
        adminOnly = true
    ) { ProgrammingCard(it.kkm, it.kkmActions) },

    // Печатная форма — настройка кассы, и меняет её только администратор
    // в режиме программирования. Кассиру она показывалась карточкой,
    // в которой мертво всё: войти в режим он тоже не может.
    SettingsCard(
        Setting.PrintForm,
        SettingsGroup.Printing,
        needsRegister = true,
        adminOnly = true
    ) { PrintFormCard(it.form, it.formActions) },
    SettingsCard(Setting.PrintTarget, SettingsGroup.Printing, needsRegister = true) {
        it.parts.printTarget()
    },

    // Отрасль стоит перед налогами: и то и другое решает, чем будет
    // наполнен каждый чек этой кассы, и владелец задаёт их в одном месте.
    // Кассиру выбор не показывается: отрасль задают при настройке кассы,
    // а не по ходу смены.
    SettingsCard(
        Setting.Domain,
        SettingsGroup.Trade,
        needsRegister = true,
        adminOnly = true
    ) { TradeDomainCard(it.workplace, it.workplaceActions) },

    SettingsCard(
        Setting.Tax,
        SettingsGroup.Service,
        needsRegister = true,
        adminOnly = true
    ) { TaxSettingsCard(it.tax, it.taxActions) },
    SettingsCard(
        Setting.OfdSync,
        SettingsGroup.Service,
        needsRegister = true,
        adminOnly = true
    ) { OfdSyncCard(it.ofd, it.ofdActions) },
    SettingsCard(
        Setting.OfdToken,
        SettingsGroup.Service,
        needsRegister = true,
        adminOnly = true
    ) { OfdTokenCard(it.ofd, it.ofdActions) },
    SettingsCard(Setting.Diagnostics, SettingsGroup.Service, needsRegister = true) {
        DiagnosticsCard(it.ofd, it.ofdActions)
    },

    SettingsCard(
        Setting.Decommission,
        SettingsGroup.Irreversible,
        needsRegister = true,
        adminOnly = true
    ) { DecommissionCard(it.kkm, it.kkmActions) }
)
