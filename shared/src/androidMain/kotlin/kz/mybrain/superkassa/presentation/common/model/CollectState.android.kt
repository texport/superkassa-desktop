package kz.mybrain.superkassa.presentation.common.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow

/** Android: сбор идёт, пока экран виден; свёрнутое приложение не пересчитывает состояние. */
@Composable
actual fun <T> StateFlow<T>.collectAsScreenState(): State<T> = collectAsStateWithLifecycle()
