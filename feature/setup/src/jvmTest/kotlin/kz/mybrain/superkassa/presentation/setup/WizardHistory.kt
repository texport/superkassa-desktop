package kz.mybrain.superkassa.presentation.setup

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.navigation3.runtime.NavKey
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.navigation.LocalNavigator
import kz.mybrain.superkassa.navigation.Navigator
import kz.mybrain.superkassa.navigation.section.RegisterKey
import kz.mybrain.superkassa.navigation.step.SetupStepKey

/**
 * История окна для мастера в проверке — как у каркаса: раздел «Новая
 * касса» в основании, шаги мастера поверх него.
 *
 * Шаги показываются так же, как их показывает `NavDisplay` окна: вершина
 * истории, у каждой записи — своё сохраняемое состояние, которое живёт,
 * пока запись в истории.
 */
internal class WizardHistory(vararg steps: String) {
    val keys = mutableStateListOf<NavKey>(RegisterKey).apply { steps.forEach { add(SetupStepKey(it)) } }

    /** Имена шагов поверх первого, по порядку. */
    val steps: List<String> get() = keys.drop(1).map { (it as SetupStepKey).step }

    val navigator = object : Navigator {
        override fun open(key: NavKey) {
            keys.add(key)
        }

        override fun back() {
            if (keys.size > 1) keys.removeAt(keys.lastIndex)
        }

        override fun close(key: NavKey) {
            val at = keys.lastIndexOf(key)
            if (at > 0) keys.subList(at, keys.size).clear()
        }
    }
}

/** Вершина истории [history]: шаг мастера, нарисованный [shown]. */
@Composable
internal fun WizardShown(history: WizardHistory, shown: @Composable (step: String?) -> Unit) {
    val holder = rememberSaveableStateHolder()
    CompositionLocalProvider(LocalNavigator provides history.navigator) {
        val top = history.keys.last()
        key(top) {
            holder.SaveableStateProvider(top.toString()) { shown((top as? SetupStepKey)?.step) }
        }
    }
}

/** Нажимается ли кнопка с надписью [label]: ищется узел кнопки, а не её надпись. */
internal fun RenderProbe.pressable(label: String): Boolean = nodes { all ->
    val button = all.first { node ->
        node.config.getOrNull(SemanticsProperties.Role) == Role.Button && node.says(label)
    }
    !button.config.contains(SemanticsProperties.Disabled)
}

private fun SemanticsNode.says(label: String): Boolean =
    config.getOrNull(SemanticsProperties.Text)?.any { it.text == label } == true || children.any { it.says(label) }
