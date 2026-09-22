package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.ScrollableColumn
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.SettingStrings
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Настройки — один экран на всё приложение.
 *
 * Открывается из двух мест: с экрана входа, где кассы ещё нет, и из кассы,
 * куда кассир вошёл. Прежде это были два разных экрана со своими списками
 * карточек, и они разошлись: отладка стояла только за входом — то есть
 * ровно там, где она уже не нужна, потому что войти получилось.
 *
 * Список карточек один, а показывается каждая по своим условиям:
 * настройке кассы нужна выбранная касса, служебной — права
 * администратора. До входа ни того, ни другого нет, и остаётся то, что
 * задают раньше, чем куда-либо войти: вид, адреса узла и кабинета,
 * службы карты и отладка.
 *
 * Порядок идёт от повседневного к необратимому: снятие с учёта
 * последним, чтобы его не нажимали по дороге к диагностике.
 */
@Composable
fun SettingsScreen(session: Session) {
    val texts = LocalStrings.current.settings
    ScrollableColumn(
        modifier = Modifier.fillMaxSize().padding(Spacing.screen),
        spacing = Spacing.roomy
    ) {
        Text(texts.title, style = MaterialTheme.typography.headlineSmall)
        SettingsCards(session)
    }
}

/**
 * Карточки настроек, отобранные по месту и правам.
 *
 * Вынесены отдельно от экрана: тот же набор стоит в окне настроек
 * рабочего места, где у него своя шапка с возвратом.
 */
@Composable
fun SettingsCards(session: Session) {
    val texts = LocalStrings.current.settings
    val shown = settingsCards.filter { it.visible(session.selected != null, session.isAdmin) }
    SettingsGroup.entries.forEach { group ->
        val cards = shown.filter { it.group == group }
        if (cards.isEmpty()) return@forEach
        group.title(texts)?.let { GroupTitle(it) }
        cards.forEach { it.card(session) }
    }
}

/** Группы настроек в порядке от повседневного к необратимому. */
private enum class SettingsGroup(val title: (SettingStrings) -> String?) {

    /** Касса, с которой работает это рабочее место: без заголовка, она одна. */
    Current({ null }),

    Appearance({ it.groupAppearance }),
    Service({ it.groupService }),
    Irreversible({ it.groupIrreversible })
}

/**
 * Имя настройки.
 *
 * Нужно, чтобы правило показа можно было проверить, не рисуя экран:
 * отбор возвращает имена, а не карточки.
 */
internal enum class Setting {
    CurrentKkm, Appearance, PrintForm, PrintTarget, PanelBehaviour,
    Tax, OfdSync, NodeAddress, CabinetAddress, MapServices, OfdToken,
    Diagnostics, Debug, NodeFacts, Updates, Decommission
}

/**
 * Настройка: своя карточка и условия показа.
 *
 * @param needsRegister настройке нужна выбранная касса.
 * @param adminOnly узел отвечает по ней только администратору.
 */
private class SettingsCard(
    val setting: Setting,
    val group: SettingsGroup,
    val needsRegister: Boolean = false,
    val adminOnly: Boolean = false,
    val card: @Composable (Session) -> Unit
) {
    fun visible(hasRegister: Boolean, admin: Boolean): Boolean =
        (!needsRegister || hasRegister) && (!adminOnly || admin)
}

/** Что видно при таком месте и таких правах. */
internal fun visibleSettings(hasRegister: Boolean, admin: Boolean): List<Setting> =
    settingsCards.filter { it.visible(hasRegister, admin) }.map { it.setting }

/**
 * Весь список настроек приложения.
 *
 * Одно место, где видно, что у кассы вообще настраивается и что кому
 * доступно. Добавленная карточка ложится сюда со своими условиями,
 * а не в один из двух экранов, как было раньше.
 */
private val settingsCards = listOf(
    SettingsCard(Setting.CurrentKkm, SettingsGroup.Current, needsRegister = true) { CurrentKkmCard(it) },

    SettingsCard(Setting.Appearance, SettingsGroup.Appearance) { AppearanceCard(it) },
    SettingsCard(Setting.PrintForm, SettingsGroup.Appearance, needsRegister = true) { PrintFormCard(it) },
    SettingsCard(Setting.PrintTarget, SettingsGroup.Appearance, needsRegister = true) { PrintTargetCard(it) },
    SettingsCard(Setting.PanelBehaviour, SettingsGroup.Appearance) { PanelBehaviourCard(it) },

    SettingsCard(Setting.Tax, SettingsGroup.Service, needsRegister = true, adminOnly = true) { TaxSettingsCard(it) },
    SettingsCard(Setting.OfdSync, SettingsGroup.Service, needsRegister = true, adminOnly = true) { OfdSyncCard(it) },
    // Адреса узла и кабинета задают раньше, чем куда-либо входят: кассе
    // без адреса узла войти некуда.
    SettingsCard(Setting.NodeAddress, SettingsGroup.Service) { NodeAddressCard(it) },
    SettingsCard(Setting.CabinetAddress, SettingsGroup.Service) { CabinetAddressCard(it) },
    SettingsCard(Setting.MapServices, SettingsGroup.Service) { MapServicesCard(it) },
    SettingsCard(Setting.OfdToken, SettingsGroup.Service, needsRegister = true, adminOnly = true) { OfdTokenCard(it) },
    SettingsCard(Setting.Diagnostics, SettingsGroup.Service, needsRegister = true) { DiagnosticsCard(it) },
    // Отладка нужна ровно тогда, когда войти нельзя: узел не отвечает,
    // список касс пуст. Условий у неё нет намеренно.
    SettingsCard(Setting.Debug, SettingsGroup.Service) { DebugCard(it) },
    SettingsCard(Setting.NodeFacts, SettingsGroup.Service) { NodeFactsCard(it) },
    // Версия кассы и выпуски не зависят ни от кассы, ни от прав: узнать,
    // что стоит и что вышло, можно с экрана входа.
    SettingsCard(Setting.Updates, SettingsGroup.Service) { UpdatesCard(it) },

    SettingsCard(
        Setting.Decommission,
        SettingsGroup.Irreversible,
        needsRegister = true,
        adminOnly = true
    ) { DecommissionCard(it) }
)

/**
 * Заголовок группы настроек.
 *
 * Карточек в настройках десяток, и без групп они читаются одним списком:
 * кассир ищет нужную глазами по всему экрану.
 */
@Composable
private fun GroupTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
