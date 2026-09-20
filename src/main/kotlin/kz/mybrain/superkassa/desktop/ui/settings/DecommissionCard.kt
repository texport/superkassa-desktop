package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.refreshKkms
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.ConfirmDangerDialog
import kz.mybrain.superkassa.desktop.ui.strings.KkmSetupTexts
import kz.mybrain.superkassa.desktop.ui.strings.moneyTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Снятие кассы с учёта.
 *
 * Раздел выделен обводкой цвета отказа и стоит последним: он необратим,
 * и всё в нём — от рамки до кнопки — должно останавливать руку, а не
 * подгонять её.
 *
 * Требования узла показаны отдельными плашками и посчитаны здесь же:
 * узел откажет ровно по ним, и кассир должен видеть, чего не хватает,
 * до нажатия, а не после четырёх отказов подряд.
 */
@Composable
fun DecommissionCard(session: Session) {
    val money = moneyTexts(session.language).kkm
    val scope = rememberCoroutineScope()
    val kkm = session.selected ?: return
    var asked by remember { mutableStateOf(false) }

    val requirements = listOf(
        money.needProgramming to (kkm.state == PROGRAMMING),
        money.needShiftClosed to !session.shiftOpen,
        money.needQueueEmpty to session.queueTasks.none { it.isWaiting },
        money.needOnline to !kkm.isAutonomous
    )
    val ready = requirements.all { it.second }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        border = CardDefaults.outlinedCardBorder().copy(brush = errorEdge())
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.roomy),
            verticalArrangement = Arrangement.spacedBy(Spacing.snug)
        ) {
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
            // Требования переносятся, а не жмутся в строку: перенос
            // оставляет на виду все четыре, в том числе невыполненное.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
                verticalArrangement = Arrangement.spacedBy(Spacing.hairline),
                itemVerticalAlignment = Alignment.CenterVertically
            ) {
                requirements.forEach { (title, met) ->
                    Chip(title, if (met) StatusColors.delivered else StatusColors.refused)
                }
            }
            OutlinedButton(
                enabled = ready,
                onClick = { asked = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) { Text(money.decommission) }
        }
    }

    if (asked) {
        ConfirmDangerDialog(
            what = money.decommissionConfirm.format(session.displayName(kkm)),
            explain = money.decommissionHint,
            action = money.decommission,
            cancel = moneyTexts(session.language).drawer.cancel,
            onCancel = { asked = false },
            onConfirm = {
                asked = false
                scope.launch { decommission(session, money) }
            }
        )
    }
}

/** Обводка раздела цветом отказа. */
@Composable
private fun errorEdge() = SolidColor(MaterialTheme.colorScheme.error)

/**
 * Снимает кассу с учёта и уводит на выбор кассы.
 *
 * Оставаться на экране настроек удалённой кассы нельзя: каждый следующий
 * запрос отвечал бы «касса не найдена». Сообщение объявляется последним —
 * смена кассы сбрасывает предыдущее.
 */
private suspend fun decommission(session: Session, money: KkmSetupTexts) {
    val kkm = session.selected ?: return
    session.guard(money.decommission) {
        session.client.decommissionKkm(kkm.kkmId, session.pin)
    } ?: return
    session.refreshKkms()
    session.switchKkm()
    session.report(money.decommissionDone)
}
