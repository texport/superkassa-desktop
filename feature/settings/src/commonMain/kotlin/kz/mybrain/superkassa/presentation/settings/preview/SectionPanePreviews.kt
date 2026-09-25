package kz.mybrain.superkassa.presentation.settings.preview

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.preview.PanePreviews
import kz.mybrain.superkassa.designsystem.preview.PreviewTheme
import kz.mybrain.superkassa.presentation.settings.SectionPane
import kz.mybrain.superkassa.presentation.settings.SettingsSection

/*
 * Каждый раздел настроек — панелью подробностей, открытой поверх списка,
 * как на телефоне: со стрелкой назад. Разделы из групп других областей —
 * экран продажи и отладка — здесь пусты: их группы показывают превью
 * своих областей.
 */

@PanePreviews
@Composable
private fun GeneralSectionPreview() = Section(SettingsSection.General)

@PanePreviews
@Composable
private fun PrintingSectionPreview() = Section(SettingsSection.Printing)

@PanePreviews
@Composable
private fun TaxesSectionPreview() = Section(SettingsSection.Taxes)

@PanePreviews
@Composable
private fun BfdSectionPreview() = Section(SettingsSection.Bfd)

@PanePreviews
@Composable
private fun LookSectionPreview() = Section(SettingsSection.Look)

@PanePreviews
@Composable
private fun LanguageSectionPreview() = Section(SettingsSection.Language)

@PanePreviews
@Composable
private fun AboutSectionPreview() = Section(SettingsSection.About)

@PanePreviews
@Composable
private fun DeliverySectionPreview() = Section(SettingsSection.Delivery)

@PanePreviews
@Composable
private fun ConnectionSectionPreview() = Section(SettingsSection.Connection)

@Composable
private fun Section(section: SettingsSection) = PreviewTheme {
    SectionPane(SettingsSamples.admin(), section, onBack = {})
}
