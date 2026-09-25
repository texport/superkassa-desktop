package kz.mybrain.superkassa.presentation.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.setup.SetupTexts

/**
 * Как идёт мастер и «Начать заново».
 *
 * Ни названия, ни возврата здесь нет: «Новая касса» и стрелку назад
 * рисует шапка окна — и в разделе кассы, и за дверью экрана входа, —
 * а мастер второго заголовка под ней не строит.
 *
 * Пока мастер ничего не прошёл, забывать нечего, и кнопки нет: нажатая
 * по ошибке, она стирает заводской номер, уже унесённый в кабинет.
 */
@Composable
internal fun SetupHeading(setup: SetupTexts, started: Boolean, actions: SetupActions) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = setup.explain,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (started) {
            TextButton(onClick = { actions.askStartOver(true) }) { Text(setup.startOver) }
        }
    }
}
