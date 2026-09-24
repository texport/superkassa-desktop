package kz.mybrain.superkassa.presentation.debug.log

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import kz.mybrain.superkassa.domain.debug.model.LogLevel
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.picker.SwitchRow
import kz.mybrain.superkassa.presentation.common.picker.WideChoiceSegments
import kz.mybrain.superkassa.presentation.common.section.PartTitle
import kz.mybrain.superkassa.presentation.common.section.SectionCard
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.debug.debugTexts
import kz.mybrain.superkassa.presentation.strings.debug.name

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
internal fun DebugCard(journal: LogUiState, actions: LogActions) {
    val texts = debugTexts(LocalLanguage.current)
    SectionCard(title = texts.debugMode, info = texts.debugModeHint) {
        SwitchRow(texts.title, journal.book.debugMode, actions::switchDebugMode)
        PartTitle(texts.level)
        WideChoiceSegments(
            options = LogLevel.entries,
            selected = journal.book.level,
            label = { texts.name(it) },
            onSelect = actions::chooseLevel
        )
        journal.book.file?.let { file -> Note("${texts.file}: $file") }
        Note(texts.secretsHint)
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

/** Карточка режима отладки со своей моделью — для настроек, которые о журнале не знают. */
@Composable
fun DebugSetting(app: AppContainer) {
    val model = logViewModel(app)
    val state by model.state.collectAsScreenState()
    DebugCard(state, model)
}
