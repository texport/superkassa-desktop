package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.components.ChoiceSegments
import kz.mybrain.superkassa.desktop.ui.strings.HistoryJournalTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Срок журнала: какой длины и где стоит.
 *
 * Сегменты выбирают длину — день, неделя, месяц, всё время, — а стрелки
 * двигают окно этой длины назад и вперёд. Кассир ищет «вчера», владелец —
 * «прошлую неделю», и оба получают это двумя нажатиями, не набирая дат.
 *
 * Вперёд дальше сегодняшнего дня идти некуда: документов из будущего
 * не бывает, и стрелка на краю гаснет, а не отдаёт пустой список.
 *
 * Один на журнал кассы и на документы кассы в кабинете: узлу срок уходит
 * границами в миллисекундах, кабинету — датами отбора, но выбирают его
 * одинаково.
 */
@Composable
fun JournalPeriodBar(
    journal: HistoryJournalTexts,
    period: JournalPeriod,
    loading: Boolean,
    onPeriod: (JournalPeriod) -> Unit
) {
    // Полоса переносится, а не сжимается: в окне сводки одной кассы ей
    // не хватало ширины, и «Сегодня» вставало столбиком из отдельных букв —
    // надпись, которую владелец не прочитал. Читаться она обязана при любой
    // ширине окна, а перенос строки этому не мешает.
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = journal.period,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            softWrap = false
        )
        ChoiceSegments(
            options = JournalSpan.entries,
            selected = period.span,
            label = { it.title(journal) },
            enabled = !loading,
            onSelect = { onPeriod(JournalPeriod.of(it)) }
        )
        PeriodShift(journal, period, loading, onPeriod)
    }
}

/** Перелистывание срока: назад, вперёд и «к сегодняшнему». */
@Composable
private fun PeriodShift(
    journal: HistoryJournalTexts,
    period: JournalPeriod,
    loading: Boolean,
    onPeriod: (JournalPeriod) -> Unit
) {
    // У «всего времени» границ нет, и листать в нём нечего: стрелки
    // при нём не показываются вовсе, чтобы не обещать движения.
    if (period.range == null) return
    val later = period.hasLater()
    IconButton(enabled = !loading, onClick = { onPeriod(period.shiftedBy(-1)) }) {
        Icon(AppIcons.earlierDay, contentDescription = journal.earlierSpan)
    }
    Text(period.text(journal), style = MaterialTheme.typography.titleMedium, softWrap = false)
    IconButton(enabled = !loading && later, onClick = { onPeriod(period.shiftedBy(1)) }) {
        Icon(AppIcons.laterDay, contentDescription = journal.laterSpan)
    }
    TextButton(enabled = !loading && later, onClick = { onPeriod(JournalPeriod.of(period.span)) }) {
        Icon(AppIcons.today, contentDescription = null)
        // Надпись не переносится по буквам ни при какой ширине: перенос
        // ряда — дело полосы, а не отдельной кнопки внутри неё.
        Text(journal.today, modifier = Modifier.padding(start = Spacing.tight), softWrap = false)
    }
}
