package kz.mybrain.superkassa.designsystem.wizard

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.picker.RadioRows
import kz.mybrain.superkassa.designsystem.preview.PreviewTheme
import kz.mybrain.superkassa.designsystem.preview.ScreenPreviews
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons

/*
 * Шаг мастера во всех классах окна: выбор из вариантов и поля ввода.
 * Слова — образцы: у дизайн-системы своих надписей экранов нет.
 */

@ScreenPreviews
@Composable
private fun WizardChoicePreview() = PreviewTheme {
    WizardPage(
        progress = WizardProgress(FIRST, FIVE, "Шаг 1 из 5"),
        heading = WizardHeading(AppIcons.setupWay, "Как подключить кассу", SAMPLE_EXPLAIN),
        leading = {},
        trailing = { Button(onClick = {}) { Text("Далее") } }
    ) {
        RadioRows(listOf("Через кабинет БФД", "Вручную"), "Через кабинет БФД", { it }, { SAMPLE_HINT }) {}
    }
}

@ScreenPreviews
@Composable
private fun WizardFieldsPreview() = PreviewTheme {
    WizardPage(
        progress = WizardProgress(THIRD, FOUR, "Шаг 3 из 4"),
        heading = WizardHeading(AppIcons.setupCredentials, "Идентификатор и токен", SAMPLE_EXPLAIN),
        leading = { TextButton(onClick = {}) { Text("Назад") } },
        trailing = { Button(onClick = {}, enabled = false) { Text("Далее") } }
    ) {
        OutlinedTextField("", {}, label = { Text("Идентификатор кассы") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField("", {}, label = { Text("Токен") }, modifier = Modifier.fillMaxWidth())
    }
}

/** Ход образцов: первый шаг из пяти и третий из четырёх. */
private const val FIRST = 1
private const val THIRD = 3
private const val FOUR = 4
private const val FIVE = 5

private const val SAMPLE_EXPLAIN = "Два-три предложения о том, что происходит на шаге и зачем он нужен. " +
    "Мастер можно закрыть на любом шаге — пройденное сохранится."

private const val SAMPLE_HINT = "Объяснение варианта строкой под его подписью"
