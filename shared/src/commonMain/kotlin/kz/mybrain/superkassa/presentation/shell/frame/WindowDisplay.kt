package kz.mybrain.superkassa.presentation.shell.frame

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import kz.mybrain.superkassa.designsystem.theme.motion.ScreenMotion
import kz.mybrain.superkassa.designsystem.theme.motion.rememberScreenMotion

/**
 * Вершина истории окна, нарисованная `NavDisplay`, — одна на оба окна:
 * рабочее и до входа.
 *
 * Разделы сменяют друг друга переходом Material 3 «fade through», шаг
 * внутри раздела — «shared axis X» ([ScreenMotion]); прежде это был
 * долгий наплыв, и уходящий экран был виден сквозь новый. Escape — шаг
 * назад по той же истории, что жест Android и стрелка в шапке.
 *
 * @param entries экраны по ключам; шагам внутри разделов — метаданные
 *   перехода вглубь.
 */
@Composable
internal fun WindowDisplay(
    history: MutableList<NavKey>,
    back: () -> Unit,
    modifier: Modifier,
    entries: EntryProviderScope<NavKey>.(step: Map<String, Any>) -> Unit
) {
    val motion = rememberScreenMotion()
    val step = remember(motion) { stepMotion(motion) }
    NavDisplay(
        backStack = history,
        modifier = modifier.backOnEscape(history.size > 1, back),
        onBack = back,
        transitionSpec = { motion.fadeThrough() },
        popTransitionSpec = { motion.fadeThrough() },
        predictivePopTransitionSpec = { motion.fadeThrough() },
        entryProvider = entryProvider { entries(step) }
    )
}

/** Переход шага вглубь и обратно — для метаданных записи шага. */
private fun stepMotion(motion: ScreenMotion): Map<String, Any> =
    NavDisplay.transitionSpec { motion.forward() } +
        NavDisplay.popTransitionSpec { motion.backward() } +
        NavDisplay.predictivePopTransitionSpec { motion.backward() }
