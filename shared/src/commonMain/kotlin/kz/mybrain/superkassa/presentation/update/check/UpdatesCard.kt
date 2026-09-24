package kz.mybrain.superkassa.presentation.update.check

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import kz.mybrain.superkassa.domain.update.model.UpdateOutcome
import kz.mybrain.superkassa.presentation.common.adaptive.WrapRow
import kz.mybrain.superkassa.presentation.common.button.BusyButton
import kz.mybrain.superkassa.presentation.common.button.FieldButtonKind
import kz.mybrain.superkassa.presentation.common.format.Dates
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.picker.SwitchRow
import kz.mybrain.superkassa.presentation.common.section.FactLines
import kz.mybrain.superkassa.presentation.common.section.SectionCard
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.update.UpdateTexts
import kz.mybrain.superkassa.presentation.strings.update.updateTexts
import kz.mybrain.superkassa.presentation.theme.size.Spacing

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
internal fun UpdatesCard(updates: UpdatesUiState, actions: UpdatesActions) {
    val texts = updateTexts(LocalLanguage.current)
    SectionCard(title = texts.title, info = texts.hint) {
        SwitchRow(texts.automatic, updates.automatic, actions::switchAutomatic)
        FactLines(texts.appName, factLines(updates, texts), texts.neverChecked)
        // Итог проверки переносится под кнопку целиком, когда ему не хватает
        // строки: «Сервер выпусков недоступен» по-казахски рядом с кнопкой
        // сжимался в узкий столбик.
        WrapRow(spacing = Spacing.snug) {
            BusyButton(
                text = if (updates.checking) texts.checking else texts.checkNow,
                busy = updates.checking,
                enabled = !updates.checking,
                kind = FieldButtonKind.Tonal,
                onClick = actions::check
            )
            OutcomeWords(updates.outcome, texts)
        }
        updates.available?.let { update -> DownloadButton(updates.installing, texts) { actions.install(update) } }
    }
}

/** «Скачать»: пока установщик скачивается и сверяется, второе нажатие не принимается. */
@Composable
private fun DownloadButton(installing: Boolean, texts: UpdateTexts, onClick: () -> Unit) {
    BusyButton(
        text = if (installing) texts.downloading else texts.download,
        busy = installing,
        enabled = !installing,
        onClick = onClick
    )
}

/** Версия и время последней проверки; непроверенное так и называется. */
private fun factLines(updates: UpdatesUiState, texts: UpdateTexts): List<Pair<String, String>> = listOf(
    texts.installed to updates.version.label,
    texts.lastChecked to (updates.lastChecked?.let { Dates.moment(it.toEpochMilliseconds()) } ?: texts.neverChecked)
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

/** Карточка обновлений со своей моделью — для настроек, которые о выпусках не знают. */
@Composable
fun UpdatesSetting(app: AppContainer) {
    val model = updatesViewModel(app)
    val state by model.state.collectAsScreenState()
    UpdatesCard(state, model)
}
