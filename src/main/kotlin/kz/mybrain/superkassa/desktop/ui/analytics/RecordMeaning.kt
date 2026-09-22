package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.desktop.ui.cabinet.KkmRecord
import kz.mybrain.superkassa.desktop.ui.components.StatusTone
import kz.mybrain.superkassa.desktop.ui.components.toneColor

/**
 * Как смысл учёта показывается на вкладке: число и цвет.
 *
 * Плитки над таблицей, заголовок таблицы и её строки перечисляют одни
 * и те же четыре смысла. Выписанные в каждом месте своей рукой, они
 * разошлись бы порядком — и столбец «отказ КГД» встал бы под заголовком
 * «учёт идёт». Поэтому все три места проходят по [KkmRecord.entries],
 * а число и цвет смысла спрашивают здесь.
 *
 * Словами смысл называется там же, где и в отборе карты, — [recordTitle].
 */
fun RecordCount.of(meaning: KkmRecord): Int = when (meaning) {
    KkmRecord.OnRecord -> onRecord
    KkmRecord.InProgress -> inProgress
    KkmRecord.Refused -> refused
    KkmRecord.Deregistered -> deregistered
}

/**
 * Цвет смысла учёта.
 *
 * Тот же, каким покрашена касса в плашках и на карте: на учёте —
 * благополучие, учёт идёт — ожидание, отказ — то, что требует работы,
 * снятая с учёта — покой, а не беда.
 */
fun recordTone(meaning: KkmRecord): StatusTone = when (meaning) {
    KkmRecord.OnRecord -> StatusTone.Good
    KkmRecord.InProgress -> StatusTone.Waiting
    KkmRecord.Refused -> StatusTone.Bad
    KkmRecord.Deregistered -> StatusTone.Idle
}

/**
 * Каким цветом написано число учёта.
 *
 * Нуль пишется тише остальных: цвет здесь значит «этим надо заняться»,
 * а заниматься нулём отказов не нужно — красный нуль тревожил бы
 * владельца тем, чего нет. Правило одно на плитки и на таблицу областей:
 * разойдись они, одно и то же число читалось бы в двух местах по-разному.
 */
@Composable
fun recordPaint(value: Int, tone: StatusTone): Color =
    if (value == 0) MaterialTheme.colorScheme.onSurfaceVariant else toneColor(tone)
