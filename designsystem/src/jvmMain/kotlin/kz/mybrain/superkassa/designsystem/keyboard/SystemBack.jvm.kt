package kz.mybrain.superkassa.designsystem.keyboard

import androidx.compose.runtime.Composable

/** Настольная касса: системного «назад» у окна нет, шаг назад — стрелкой на экране. */
@Composable
actual fun SystemBack(enabled: Boolean, onBack: () -> Unit) = Unit
