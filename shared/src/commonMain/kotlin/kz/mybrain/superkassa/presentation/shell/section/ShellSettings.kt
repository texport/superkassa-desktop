package kz.mybrain.superkassa.presentation.shell.section

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.presentation.debug.log.DebugSetting
import kz.mybrain.superkassa.presentation.kassa.sale.PanelBehaviourCard
import kz.mybrain.superkassa.presentation.print.target.PrintTargetSetting
import kz.mybrain.superkassa.presentation.settings.SettingsBoard
import kz.mybrain.superkassa.presentation.settings.SettingsParts
import kz.mybrain.superkassa.presentation.settings.settingsBoard
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.frame.WindowParts
import kz.mybrain.superkassa.presentation.update.check.UpdatesSetting

/** Доска настроек окна: модели настроек, вид окна и карточки других областей. */
@Composable
internal fun settingsOf(app: AppContainer, window: WindowParts): SettingsBoard =
    settingsBoard(app.services, app.areas.settings, window.look, settingsParts(app))

/**
 * Карточки других областей среди настроек: колонка продажи, принтер кассы, выпуски, журнал.
 *
 * Каждая со своей моделью окна; настройки о печати, обновлениях и отладке
 * не знают и только ставят карточку на её место.
 */
private fun settingsParts(app: AppContainer) = SettingsParts(
    panels = { PanelBehaviourCard(app.services.memory) },
    printTarget = { PrintTargetSetting(app.services, app.areas.print) },
    updates = { UpdatesSetting(app.services, app.areas.update) },
    debug = { DebugSetting(app.services, app.areas.debug) },
    hasCabinet = app.areas.cabinet != null,
    hasReleases = app.areas.update.releases.ownReleases
)
