package kz.mybrain.superkassa.presentation.common.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import kotlinx.coroutines.flow.StateFlow

/**
 * Состояние модели для экрана — одной функцией на всех платформах.
 *
 * На Android сбор идёт, пока экран виден (`collectAsStateWithLifecycle`):
 * свёрнутое приложение не пересчитывает то, чего никто не видит.
 * На настольной кассе окно не уходит в фон так, как активность, и сбор
 * идёт, пока экран в композиции. Экраны областей зовут только эту функцию.
 */
@Composable
expect fun <T> StateFlow<T>.collectAsScreenState(): State<T>
