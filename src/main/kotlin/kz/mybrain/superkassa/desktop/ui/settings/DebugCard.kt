package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.log.AppLog
import kz.mybrain.superkassa.desktop.app.log.LogLevel
import kz.mybrain.superkassa.desktop.ui.components.ChoiceSegments
import kz.mybrain.superkassa.desktop.ui.components.SectionCard
import kz.mybrain.superkassa.desktop.ui.strings.debugTexts
import kz.mybrain.superkassa.desktop.ui.strings.name
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Журнал приложения и режим отладки.
 *
 * Уровень задаётся здесь, а не в окне журнала: он решает, что попадёт
 * в файл, а файл пишется и с закрытым окном. По умолчанию уровень
 * обычный — обращения без тел: на отладочном каждый чек прибавляет
 * к файлу килобайты, а кассиру от этого нет ничего.
 *
 * Режим отладки открывает соседнее окно с тем же журналом. Выбор
 * держится рабочего места: разбор отказа редко укладывается в один
 * запуск кассы.
 */
@Composable
internal fun DebugCard(session: Session) {
    val texts = debugTexts(session.language)
    SectionCard(title = texts.debugMode, info = texts.debugModeHint) {
        DebugSwitch(texts.title)
        Text(texts.level, style = MaterialTheme.typography.titleSmall)
        ChoiceSegments(
            options = LogLevel.entries,
            selected = AppLog.level,
            label = { texts.name(it) },
            onSelect = AppLog::chooseLevel
        )
        AppLog.file?.let { file -> Note("${texts.file}: ${file.path}") }
        Note(texts.secretsHint)
    }
}

/** Переключатель режима: с ним открывается и закрывается окно журнала. */
@Composable
private fun DebugSwitch(title: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(checked = AppLog.debugMode, onCheckedChange = AppLog::switchDebugMode)
        Text(title, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Строка пояснения под настройкой: её читают один раз и не нажимают. */
@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
