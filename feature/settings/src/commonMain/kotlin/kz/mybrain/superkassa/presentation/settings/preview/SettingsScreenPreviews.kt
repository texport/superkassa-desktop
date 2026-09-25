package kz.mybrain.superkassa.presentation.settings.preview

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.preview.PanePreviews
import kz.mybrain.superkassa.designsystem.preview.PreviewTheme
import kz.mybrain.superkassa.designsystem.preview.ScreenPreviews
import kz.mybrain.superkassa.presentation.settings.SettingsBoard
import kz.mybrain.superkassa.presentation.settings.SettingsList
import kz.mybrain.superkassa.presentation.settings.SettingsScreen
import kz.mybrain.superkassa.presentation.settings.WorkplaceSettingsScreen
import kz.mybrain.superkassa.presentation.settings.sections

/** Настройки администратора за кассой: список разделов и открытая касса. */
@ScreenPreviews
@Composable
private fun AdminSettingsPreview() = PreviewTheme { SettingsScreen(SettingsSamples.admin()) }

/** Настройки кассира: разделы без служебного. */
@ScreenPreviews
@Composable
private fun CashierSettingsPreview() = PreviewTheme { SettingsScreen(SettingsSamples.cashier()) }

/** Настройки с экрана входа: кассы нет, остаётся то, что задают до входа. */
@ScreenPreviews
@Composable
private fun DoorSettingsPreview() = PreviewTheme { WorkplaceSettingsScreen(SettingsSamples.door()) }

/** Список разделов администратора — панель слева или весь экран телефона. */
@PanePreviews
@Composable
private fun AdminListPreview() = PreviewTheme { ListOf(SettingsSamples.admin()) }

/** Список разделов до входа: полки кассы нет. */
@PanePreviews
@Composable
private fun DoorListPreview() = PreviewTheme { ListOf(SettingsSamples.door()) }

@Composable
private fun ListOf(board: SettingsBoard) {
    SettingsList(board, board.sections, open = board.sections.first(), titled = true) {}
}
