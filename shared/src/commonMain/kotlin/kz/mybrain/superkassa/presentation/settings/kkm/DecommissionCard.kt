package kz.mybrain.superkassa.presentation.settings.kkm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import kz.mybrain.superkassa.domain.settings.model.KkmSettingRules
import kz.mybrain.superkassa.presentation.common.dialog.ConfirmDangerDialog
import kz.mybrain.superkassa.presentation.common.strings.LocalLanguage
import kz.mybrain.superkassa.presentation.settings.SettingRequirements
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.fill
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Снятие кассы с учёта.
 *
 * Раздел выделен обводкой цвета отказа и стоит последним: он необратим,
 * и всё в нём — от рамки до кнопки — должно останавливать руку, а не
 * подгонять её.
 *
 * Требования кассы показаны отдельными плашками: касса откажет ровно
 * по ним, и кассир должен видеть, чего не хватает, до нажатия, а не после
 * четырёх отказов подряд.
 */
@Composable
fun DecommissionCard(kkm: KkmSettingsUiState, actions: KkmSettingsActions) {
    val money = textsOf(LocalLanguage.current).kassa.money.kkm
    val current = kkm.kkm ?: return
    val needs = KkmSettingRules.decommission(current)
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = CardDefaults.outlinedCardBorder().copy(brush = errorEdge())
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
        ) {
            DecommissionHeading()
            SettingRequirements(needs, money)
            OutlinedButton(
                enabled = KkmSettingRules.met(needs) && !kkm.busy,
                onClick = actions::askDecommission,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) { Text(money.decommission) }
        }
    }
    if (kkm.decommissionAsked) DecommissionQuestion(kkm.displayName, actions)
}

/** Что сделает снятие: названо цветом отказа и объяснено до нажатия. */
@Composable
private fun DecommissionHeading() {
    val money = textsOf(LocalLanguage.current).kassa.money.kkm
    Text(
        text = money.decommission,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.error
    )
    Text(
        text = money.decommissionHint,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
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

/** Обводка раздела цветом отказа. */
@Composable
private fun errorEdge() = SolidColor(MaterialTheme.colorScheme.error)
