package kz.mybrain.superkassa.presentation.settings.kkm

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.dialog.ConfirmDangerDialog
import kz.mybrain.superkassa.designsystem.section.SettingGroup
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.domain.settings.model.KkmSettingRules
import kz.mybrain.superkassa.presentation.settings.SettingRequirements
import kz.mybrain.superkassa.strings.api.fill
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Снятие кассы с учёта.
 *
 * Группа стоит последней в разделе кассы, подзаголовок и кнопка — цвета
 * отказа: действие необратимо, и всё в группе должно останавливать руку,
 * а не подгонять её. Перед снятием — диалог подтверждения Material 3.
 *
 * Требования кассы показаны отдельными плашками: касса откажет ровно
 * по ним, и кассир должен видеть, чего не хватает, до нажатия, а не после
 * четырёх отказов подряд.
 */
@Composable
internal fun DecommissionCard(kkm: KkmSettingsUiState, actions: KkmSettingsActions) {
    val money = textsOf(LocalLanguage.current).kassa.money.kkm
    val current = kkm.kkm ?: return
    val needs = KkmSettingRules.decommission(current)
    SettingGroup(title = money.decommission, danger = true) {
        Text(
            text = money.decommissionHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SettingRequirements(needs, money)
        OutlinedButton(
            enabled = KkmSettingRules.met(needs) && !kkm.busy,
            onClick = actions::askDecommission,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) { Text(money.decommission) }
    }
    if (kkm.decommissionAsked) DecommissionQuestion(kkm.displayName, actions)
}

/** Вопрос перед необратимым: что будет удалено и с какой кассой. */
@Composable
private fun DecommissionQuestion(name: String, actions: KkmSettingsActions) {
    val money = textsOf(LocalLanguage.current).kassa.money
    ConfirmDangerDialog(
        what = money.kkm.decommissionConfirm.fill(name),
        explain = money.kkm.decommissionHint,
        action = money.kkm.decommission,
        cancel = money.drawer.cancel,
        onCancel = actions::cancelDecommission,
        onConfirm = actions::decommission
    )
}
