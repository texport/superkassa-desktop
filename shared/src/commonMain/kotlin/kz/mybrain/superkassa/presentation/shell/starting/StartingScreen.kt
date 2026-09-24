package kz.mybrain.superkassa.presentation.shell.starting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Экран, который касса показывает, пока поднимается.
 *
 * Общий для всех платформ: на нём приложение для Android впервые
 * показывает оформление и надписи настольной кассы. Рабочих разделов
 * на телефоне пока нет — их принесёт работа с ядром в процессе.
 */
@Composable
fun StartingScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(Spacing.roomy, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Text(LocalStrings.current.common.starting, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
