package kz.mybrain.superkassa.presentation.settings.preview

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.preview.ElementPreviews
import kz.mybrain.superkassa.designsystem.preview.PreviewTheme
import kz.mybrain.superkassa.presentation.settings.SettingsBoard
import kz.mybrain.superkassa.presentation.settings.kkm.CurrentKkmCard
import kz.mybrain.superkassa.presentation.settings.kkm.DecommissionCard
import kz.mybrain.superkassa.presentation.settings.kkm.KkmSettingsActions
import kz.mybrain.superkassa.presentation.settings.kkm.ProgrammingCard
import kz.mybrain.superkassa.presentation.settings.receipt.PrintFormCard
import kz.mybrain.superkassa.presentation.settings.tax.TaxSettingsCard
import kz.mybrain.superkassa.presentation.settings.workplace.TradeDomainCard

/*
 * Группы разделов самой кассы по одной: касса, режим, снятие, печатная
 * форма, отрасль и налоги.
 */

@ElementPreviews
@Composable
private fun CurrentKkmPreview() = Group { CurrentKkmCard(it.kkm, it.kkmActions) }

@ElementPreviews
@Composable
private fun ProgrammingOnPreview() = Group { ProgrammingCard(it.kkm, it.kkmActions) }

@ElementPreviews
@Composable
private fun ProgrammingOffPreview() = Group(SettingsSamples.admin(SettingsSamples.working)) {
    ProgrammingCard(it.kkm, it.kkmActions)
}

@ElementPreviews
@Composable
private fun DecommissionPreview() = Group { DecommissionCard(it.kkm, NO_KKM_ACTIONS) }

@ElementPreviews
@Composable
private fun PrintFormPreview() = Group { PrintFormCard(it.form, it.formActions) }

@ElementPreviews
@Composable
private fun TradeDomainPreview() = Group { TradeDomainCard(it.workplace, it.workplaceActions) }

@ElementPreviews
@Composable
private fun TaxPreview() = Group { TaxSettingsCard(it.tax, it.taxActions) }

/** Группа на доске администратора: тема, надписи и класс окна — как в окне кассы. */
@Composable
internal fun Group(board: SettingsBoard = SettingsSamples.admin(), content: @Composable (SettingsBoard) -> Unit) =
    PreviewTheme { content(board) }

private val NO_KKM_ACTIONS = object : KkmSettingsActions {}
