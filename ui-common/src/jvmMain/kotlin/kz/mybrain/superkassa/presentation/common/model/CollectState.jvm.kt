package kz.mybrain.superkassa.presentation.common.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow

/** Настольная касса: сбор идёт, пока экран в композиции. */
@Composable
actual fun <T> StateFlow<T>.collectAsScreenState(): State<T> = collectAsState()
