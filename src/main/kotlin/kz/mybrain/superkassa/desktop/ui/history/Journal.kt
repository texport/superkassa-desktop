package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Общее для журнала, возврата и очереди.
 *
 * Три экрана — один товароучётный журнал, разрезанный по задачам кассира,
 * и повторять в каждом тире и разбор времени незачем. Ожидание, пустота
 * и отказ здесь не живут: они одни на всё приложение и лежат в общем
 * `ScreenSlot`.
 */

/** Подложка строки журнала: через одну, чтобы глаз не терял строку. */
@Composable
fun rowTint(striped: Boolean) =
    if (striped) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surface

/**
 * Значение, которого нет.
 *
 * Своего знака у журнала нет: прочерк один на всё приложение и объявлен
 * в [Glyphs]. Имя остаётся здесь, пока на него ссылается сводка; когда
 * и она перейдёт на общий знак, строка уйдёт.
 */
const val DASH: String = Glyphs.DASH

/** Время документа по часам этой машины. */
fun momentText(millis: Long?): String =
    millis?.let { MOMENT.format(Instant.ofEpochMilli(it)) } ?: Glyphs.DASH

private val MOMENT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM HH:mm:ss").withZone(ZoneId.systemDefault())
