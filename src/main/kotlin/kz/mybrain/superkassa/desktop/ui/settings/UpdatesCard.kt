package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.UpdateOutcome
import kz.mybrain.superkassa.desktop.app.Updates
import kz.mybrain.superkassa.desktop.app.openInBrowser
import kz.mybrain.superkassa.desktop.ui.cabinet.cabinetMoment
import kz.mybrain.superkassa.desktop.ui.components.BusyButton
import kz.mybrain.superkassa.desktop.ui.components.FactLines
import kz.mybrain.superkassa.desktop.ui.components.FieldButtonKind
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.UpdateTexts
import kz.mybrain.superkassa.desktop.ui.strings.updateTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Обновления кассы.
 *
 * Установленная версия, когда проверяли и что нашли. Проверка по
 * расписанию включена по умолчанию и выключается здесь: на рабочем
 * месте без интернета она только пишет отказы в журнал.
 *
 * Итог ручной проверки — словами под кнопкой, а не всплывающей строкой:
 * владелец смотрит сюда, когда нажимает, и ответ должен остаться
 * на месте, пока он читает.
 */
@Composable
internal fun UpdatesCard(session: Session) {
    val texts = updateTexts(session.language)
    val updates = session.updates
    val scope = rememberCoroutineScope()
    var outcome by remember { mutableStateOf<UpdateOutcome?>(null) }
    SectionCard(title = texts.title, info = texts.hint) {
        AutomaticSwitch(updates, texts.automatic)
        FactLines(texts.installed, factLines(updates, texts), texts.neverChecked)
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BusyButton(
                text = if (updates.checking) texts.checking else texts.checkNow,
                busy = updates.checking,
                enabled = !updates.checking,
                kind = FieldButtonKind.Tonal,
                onClick = { scope.launch { outcome = updates.check() } }
            )
            OutcomeWords(outcome, texts)
        }
        updates.available?.let { update ->
            Button(onClick = { openInBrowser(update.download ?: update.page) }) { Text(texts.download) }
        }
    }
}

/** Проверять ли выпуски самой. */
@Composable
private fun AutomaticSwitch(updates: Updates, title: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(checked = updates.automatic, onCheckedChange = updates::switchAutomatic)
        Text(title, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Версия и время последней проверки; непроверенное так и называется. */
private fun factLines(updates: Updates, texts: UpdateTexts): List<Pair<String, String>> = listOf(
    texts.installed to updates.version.label,
    texts.lastChecked to (updates.lastChecked?.let { cabinetMoment(it.toString()) } ?: texts.neverChecked)
)

/**
 * Итог ручной проверки словами.
 *
 * Пока не нажимали — пусто, но найденная по расписанию версия всё равно
 * называется: за ней и пришли в эту карточку.
 */
@Composable
private fun OutcomeWords(outcome: UpdateOutcome?, texts: UpdateTexts) {
    val words = when (outcome) {
        null -> return
        UpdateOutcome.UpToDate -> texts.upToDate
        is UpdateOutcome.Available -> "${texts.available} ${outcome.update.version}"
        UpdateOutcome.Unreachable -> texts.unreachable
    }
    val color = if (outcome == UpdateOutcome.Unreachable) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(words, style = MaterialTheme.typography.bodyMedium, color = color)
}
