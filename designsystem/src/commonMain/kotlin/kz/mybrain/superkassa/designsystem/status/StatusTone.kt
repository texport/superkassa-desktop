package kz.mybrain.superkassa.designsystem.status

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.designsystem.theme.StatusColors

/**
 * Роль цвета плашки.
 *
 * Слово от неё не зависит — только цвет, и берётся он из схемы. Набор
 * плашек собирается без композиции, поэтому цвета в нём нет: на его
 * месте стоит роль, а в цвет её превращает [toneColor].
 */
enum class StatusTone {

    /** Так и должно быть: касса работает, смена открыта, всё доехало. */
    Good,

    /** Ждём: идёт обращение, состояние ещё неизвестно. */
    Waiting,

    /** Надо что-то сделать: отказ, блокировка, расхождение. */
    Bad,

    /**
     * Обычное состояние покоя: ни хорошо, ни плохо.
     *
     * Закрытая смена — не ожидание и не беда: так касса стоит до начала
     * дня и после Z-отчёта. Жёлтым она читалась как незаконченное дело,
     * и карточка кассы, у которой всё в порядке, выглядела тревожной.
     */
    Idle
}

/** Цвет роли: ролей три, и все три берутся из схемы, а не с места. */
@Composable
fun toneColor(tone: StatusTone): Color = when (tone) {
    StatusTone.Good -> StatusColors.delivered
    StatusTone.Waiting -> StatusColors.pending
    StatusTone.Bad -> StatusColors.refused
    StatusTone.Idle -> MaterialTheme.colorScheme.onSurfaceVariant
}
