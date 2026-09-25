package kz.mybrain.superkassa.presentation.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kz.mybrain.superkassa.navigation.LocalNavigator
import kz.mybrain.superkassa.navigation.step.StepKey

/**
 * Подробности «списка и подробностей» шагом истории окна.
 *
 * @property over подробности открыты поверх списка: одна панель и шаг открыт.
 */
class DetailStep internal constructor(
    val over: Boolean,
    private val beside: Boolean,
    private val open: (StepKey) -> Unit
) {

    /**
     * Выбрано в списке: при одной панели подробности открываются поверх
     * него шагом [key]; рядом со списком выбор шагом не становится.
     */
    fun opened(key: StepKey) {
        if (!beside) open(key)
    }
}

/**
 * Шаг подробностей по правилу Material 3 (List-detail → back navigation).
 *
 * На узком окне подробности — шаг общей истории окна: «назад» — жест,
 * Escape, стрелка в шапке — возвращает к списку. На широком они стоят
 * рядом со списком, и шага нет: раздвинули окно, пока шаг открыт, — он
 * снимается сам, и выбранное остаётся открытым рядом со списком. Шаг,
 * которому нечего показать, — выбор забыт вместе с выгруженным
 * приложением, — тоже снимается: пустые подробности вместо списка
 * ничего не дают.
 *
 * @param stepped открыт ли шаг — верх истории окна.
 * @param chosen есть ли что показать в подробностях.
 * @param beside панели стоят рядом.
 */
@Composable
fun detailStep(stepped: Boolean, chosen: Boolean, beside: Boolean): DetailStep {
    val navigator = LocalNavigator.current
    LaunchedEffect(stepped, chosen, beside) { if (stepped && (beside || !chosen)) navigator.back() }
    return DetailStep(over = stepped && chosen && !beside, beside = beside, open = navigator::open)
}
