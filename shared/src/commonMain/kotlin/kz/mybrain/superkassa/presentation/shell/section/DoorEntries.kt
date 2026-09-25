package kz.mybrain.superkassa.presentation.shell.section

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kz.mybrain.superkassa.navigation.LocalNavigator
import kz.mybrain.superkassa.navigation.section.CabinetKey
import kz.mybrain.superkassa.navigation.section.KkmsKey
import kz.mybrain.superkassa.navigation.section.RegisterKey
import kz.mybrain.superkassa.navigation.section.SettingsKey
import kz.mybrain.superkassa.navigation.step.PlaceCardKey
import kz.mybrain.superkassa.navigation.step.SettingsSectionKey
import kz.mybrain.superkassa.navigation.step.SetupStepKey
import kz.mybrain.superkassa.presentation.cabinet.signin.CabinetDoor
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.settings.WorkplaceSettingsScreen
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.frame.WindowParts
import kz.mybrain.superkassa.presentation.users.signin.LoginScreen
import kz.mybrain.superkassa.presentation.users.signin.LoginViewModel

/**
 * Разделы окна до входа по их ключам — для `NavDisplay` окна.
 *
 * Разделы собирает каркас: заведение кассы, кабинет и настройки — чужие
 * входу области, и вход о них не знает.
 *
 * @param toKkms вернуться к кассам и перечитать их — за соседним разделом
 *   кассу могли завести; этим же «перейти к кассе» уводит из кабинета.
 * @param step переход шага вглубь и обратно — метаданные записей шагов.
 */
internal fun EntryProviderScope<NavKey>.doorEntries(
    app: AppContainer,
    window: WindowParts,
    login: LoginViewModel,
    toKkms: () -> Unit,
    step: Map<String, Any>
) {
    entry<KkmsKey> { SectionPlace(app) { Kkms(app, login) } }
    entry<RegisterKey> { SectionPlace(app) { Connect(app, window, toKkms) } }
    entry<SetupStepKey>(metadata = step) { SectionPlace(app) { Connect(app, window, toKkms, it) } }
    entry<CabinetKey> { SectionPlace(app) { window.cabinet?.let { CabinetDoor(it, toKkms) } } }
    entry<PlaceCardKey>(metadata = step) {
        SectionPlace(app) { window.cabinet?.let { CabinetDoor(it, toKkms, stepped = true) } }
    }
    entry<SettingsKey> { SectionPlace(app) { WorkplaceSettingsScreen(settingsOf(app, window)) } }
    entry<SettingsSectionKey>(metadata = step) {
        SectionPlace(app) { WorkplaceSettingsScreen(settingsOf(app, window), it) }
    }
}

/**
 * Кассы и вход по пину. Когда касс нет, главное действие — «Новая касса»:
 * тот же раздел, что в навигации окна.
 */
@Composable
private fun Kkms(app: AppContainer, login: LoginViewModel) {
    val state by login.state.collectAsScreenState()
    val navigator = LocalNavigator.current
    val register = if (app.areas.setup != null) ({ navigator.open(RegisterKey) }) else null
    LoginScreen(state, login, register)
}
