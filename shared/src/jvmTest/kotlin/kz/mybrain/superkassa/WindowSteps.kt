package kz.mybrain.superkassa

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavKey
import kz.mybrain.superkassa.navigation.step.StepKey
import kz.mybrain.superkassa.presentation.shell.frame.StepsOf

/**
 * История шагов, как у окна: на узком окне подробности, нажатые в списке,
 * открываются поверх него шагом истории — раздел настроек, чек возврата,
 * карточка точки кабинета.
 *
 * @param content раздел: открытый шаг и шаг назад — `null`, пока открыт
 *   сам список.
 */
@Composable
internal fun WindowSteps(content: @Composable (step: StepKey?, back: (() -> Unit)?) -> Unit) {
    val steps = remember { mutableStateListOf<NavKey>() }
    val back = { steps.removeLastOrNull().let { } }
    StepsOf(steps, back) { content(steps.lastOrNull() as? StepKey, back.takeIf { steps.isNotEmpty() }) }
}
