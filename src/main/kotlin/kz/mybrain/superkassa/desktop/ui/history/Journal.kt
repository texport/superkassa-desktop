package kz.mybrain.superkassa.desktop.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Общее для журнала, возврата и очереди.
 *
 * Три экрана — один товароучётный журнал, разрезанный по задачам кассира,
 * и повторять в каждом пустое состояние, тире и разбор времени незачем.
 * Собственных красок здесь нет: всё берётся ролями схемы.
 */

/**
 * Значки области.
 *
 * Заведены здесь по той же причине, что и в общем наборе кассы: значок
 * меняется в одной строке, а не в каждом экране, где он встретился.
 */
object JournalIcons {
    /** Перелистывание дня. */
    val earlierDay: ImageVector = Icons.Filled.ChevronLeft
    val laterDay: ImageVector = Icons.Filled.ChevronRight
    val today: ImageVector = Icons.Filled.Today

    /** Печатная форма документа. */
    val print: ImageVector = Icons.Filled.Print

    /** Пустые состояния: нечего показать, нечего вернуть, нечего отправлять. */
    val noDocuments: ImageVector = Icons.Filled.EventBusy
    val noBasis: ImageVector = Icons.Filled.ReceiptLong
    val queueClear: ImageVector = Icons.Filled.CloudDone
}

/**
 * Размеры области.
 *
 * Задаются здесь, а не числом в экране: журнал плотный, а кассовое
 * действие крупное, и оба размера должны совпадать во всех трёх экранах.
 */
object JournalSizes {
    /**
     * Значок пустого состояния журнала.
     *
     * Мельче общего `Sizes.emptyIcon`: журнал плотный, и значок в полный
     * рост заслонял бы строку под ним. Высота действия здесь не задаётся —
     * она общая, `Sizes.fieldHeight`.
     */
    val emptyIcon = 40.dp
}

/**
 * Пустое состояние: значок, строка и подсказка.
 *
 * Пустой экран без объяснения кассир читает как поломку. Строка говорит,
 * что именно пусто, подсказка — что с этим делать.
 */
@Composable
fun JournalEmpty(icon: ImageVector, line: String, hint: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(Spacing.roomy),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.tight, Alignment.CenterVertically)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(JournalSizes.emptyIcon)
        )
        Text(line, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Ожидание ответа узла на месте будущего списка, а не строкой над ним. */
@Composable
fun JournalLoading(modifier: Modifier = Modifier) {
    val texts = LocalStrings.current
    Column(
        modifier = modifier.fillMaxWidth().padding(Spacing.roomy),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.snug, Alignment.CenterVertically)
    ) {
        CircularProgressIndicator()
        Text(
            text = texts.common.loading,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Подложка строки журнала: через одну, чтобы глаз не терял строку. */
@Composable
fun rowTint(striped: Boolean) =
    if (striped) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surface

/** Значение, которого нет: одно тире на всю область. */
const val DASH = "—"

/** Время документа по часам этой машины. */
fun momentText(millis: Long?): String =
    millis?.let { MOMENT.format(Instant.ofEpochMilli(it)) } ?: DASH

private val MOMENT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("dd.MM HH:mm:ss").withZone(ZoneId.systemDefault())
