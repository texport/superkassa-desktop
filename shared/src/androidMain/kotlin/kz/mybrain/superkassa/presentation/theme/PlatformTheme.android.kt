package kz.mybrain.superkassa.presentation.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * На телефоне и планшете полос прокрутки нет, зато есть полосы системы.
 *
 * Окно рисуется от края до края, и значки часов и заряда стоят поверх
 * шапки кассы. Их цвет следует теме кассы, а не системы: тёмная касса
 * на светлой системе иначе получала тёмные значки на тёмной шапке.
 */
@Composable
internal actual fun PlatformTheme(content: @Composable () -> Unit) {
    SystemBarsFollowTheme(LocalDarkTheme.current)
    content()
}

@Composable
private fun SystemBarsFollowTheme(dark: Boolean) {
    val view = LocalView.current
    SideEffect {
        val window = view.context.activity()?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

private fun Context.activity(): Activity? =
    generateSequence(this) { (it as? ContextWrapper)?.baseContext }.firstNotNullOfOrNull { it as? Activity }
