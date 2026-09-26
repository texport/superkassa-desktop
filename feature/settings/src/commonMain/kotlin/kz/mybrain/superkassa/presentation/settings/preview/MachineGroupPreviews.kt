package kz.mybrain.superkassa.presentation.settings.preview

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.preview.ElementPreviews
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.domain.settings.model.KkmSettingRules
import kz.mybrain.superkassa.presentation.settings.SettingRequirements
import kz.mybrain.superkassa.presentation.settings.core.CoreSettingsCard
import kz.mybrain.superkassa.presentation.settings.core.KassaFactsCard
import kz.mybrain.superkassa.presentation.settings.workplace.AppearanceCard
import kz.mybrain.superkassa.presentation.settings.workplace.CabinetAddressCard
import kz.mybrain.superkassa.presentation.settings.workplace.LanguageCard
import kz.mybrain.superkassa.presentation.settings.workplace.MapServicesCard
import kz.mybrain.superkassa.strings.api.textsOf

/*
 * Группы разделов машины по одной: оформление, язык, сведения и сроки
 * кассы на машине, доставка чека, адреса служб; и плашки требований кассы.
 */

@ElementPreviews
@Composable
private fun AppearancePreview() = Group { AppearanceCard(it.look, it.lookActions) }

@ElementPreviews
@Composable
private fun LanguagePreview() = Group { LanguageCard(it.look, it.lookActions) }

@ElementPreviews
@Composable
private fun FactsPreview() = Group { KassaFactsCard(it.core) }

@ElementPreviews
@Composable
private fun CorePreview() = Group { CoreSettingsCard(it.core, it.coreActions) }

@ElementPreviews
@Composable
private fun CabinetAddressPreview() = Group { CabinetAddressCard(it.workplace, it.workplaceActions) }

@ElementPreviews
@Composable
private fun MapServicesPreview() = Group { MapServicesCard(it.workplace, it.workplaceActions) }

@ElementPreviews
@Composable
private fun RequirementsPreview() = Group(SettingsSamples.admin(SettingsSamples.working)) {
    SettingRequirements(KkmSettingRules.tax(SettingsSamples.working), textsOf(LocalLanguage.current).kassa.money.kkm)
}
