package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.ShiftState
import kz.mybrain.superkassa.desktop.app.titleOf
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.coreTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

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

/** Плашка шапки: слово и роль цвета. */
data class KkmStatusChip(val text: String, val tone: StatusTone)

/** Слова шапки, уже переведённые на язык кассира. */
data class KkmStatusWords(
    val state: String,
    val autonomous: String,
    val shiftOpen: String,
    val shiftClosed: String,
    val shiftUnknown: String,
    val nodeOnline: String,
    val nodeOffline: String
)

/**
 * Набор плашек шапки по порядку.
 *
 * Состояние кассы названо ровно один раз — словом справочника узла.
 * Раньше рядом с ним стояла своя плашка «Заблокирована», и у кассы
 * в состоянии `BLOCKED` справочник давал то же самое слово: кассир
 * читал его дважды подряд. Оба слова брались из одного признака,
 * поэтому своя плашка не добавляла ничего — блокировка осталась
 * цветом этой же плашки.
 *
 * Собирается без композиции, чтобы повтор ловился проверкой, а не глазами.
 */
fun kkmStatusChips(
    kkm: Kkm?,
    shift: ShiftState,
    nodeAvailable: Boolean,
    words: KkmStatusWords
): List<KkmStatusChip> = listOfNotNull(
    kkm?.let { KkmStatusChip(words.state, stateTone(it.isBlocked, it.isProgramming)) },
    kkm?.takeIf { it.isAutonomous }?.let { KkmStatusChip(words.autonomous, StatusTone.Waiting) },
    kkm?.let { shiftChip(shift, words) },
    nodeChip(nodeAvailable, words)
)

/**
 * Состояние смены словами узла, и «неизвестно» — одно из них.
 *
 * Плашка знала два состояния из трёх и неизвестное называла закрытым:
 * у кассы, о смене которой узел молчит, шапка писала «Смена закрыта»
 * над сменой, которую узел держит открытой, — а рядом на главном экране
 * та же смена стояла «Неизвестна». Закрытая смена — ожидание, а не отказ.
 */
private fun shiftChip(shift: ShiftState, words: KkmStatusWords): KkmStatusChip = when (shift) {
    ShiftState.Open -> KkmStatusChip(words.shiftOpen, StatusTone.Good)
    ShiftState.Closed -> KkmStatusChip(words.shiftClosed, StatusTone.Waiting)
    ShiftState.Unknown -> KkmStatusChip(words.shiftUnknown, StatusTone.Waiting)
}

/** Связь с узлом: без неё касса не примет ни одной команды — это отказ. */
private fun nodeChip(available: Boolean, words: KkmStatusWords): KkmStatusChip =
    if (available) {
        KkmStatusChip(words.nodeOnline, StatusTone.Good)
    } else {
        KkmStatusChip(words.nodeOffline, StatusTone.Bad)
    }

/**
 * Состояние кассы — одним набором плашек и в одном месте.
 *
 * Раньше состояние кассы стояло в настройках, режим программирования —
 * в диагностике, автономная работа и связь с узлом — в шапке: кассир
 * собирал картину по трём экранам, а одно и то же слово в двух местах
 * могло разойтись. Набор объявлен здесь один раз и показывается там,
 * где нужен, — в шапке окна.
 *
 * Порядок от тревожного к обычному: состояние кассы и автономную работу
 * кассир обязан увидеть первыми.
 */
@Composable
fun KkmStatusChips(session: Session) {
    val chips = kkmStatusChips(
        kkm = session.selected,
        shift = session.shiftState,
        nodeAvailable = session.nodeAvailable,
        words = statusWords(session)
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        chips.forEach { chip -> Chip(chip.text, toneColor(chip.tone)) }
    }
}

/** Слова шапки на языке кассира; состояние называет справочник узла. */
@Composable
private fun statusWords(session: Session): KkmStatusWords {
    val texts = LocalStrings.current
    val core = coreTexts(session.language)
    return KkmStatusWords(
        state = session.titleOf(Dictionary.KkmStates, session.selected?.state),
        autonomous = texts.shell.autonomous,
        shiftOpen = core.shiftOpenShort,
        shiftClosed = core.shiftClosedShort,
        shiftUnknown = core.shiftUnknownShort,
        nodeOnline = texts.common.nodeOnline,
        nodeOffline = texts.common.nodeOffline
    )
}

/** Роль состояния кассы: блокировка — отказ, программирование — ожидание. */
fun stateTone(blocked: Boolean, programming: Boolean): StatusTone = when {
    blocked -> StatusTone.Bad
    programming -> StatusTone.Waiting
    else -> StatusTone.Good
}

/** Цвет роли: ролей три, и все три берутся из схемы, а не с места. */
@Composable
fun toneColor(tone: StatusTone): Color = when (tone) {
    StatusTone.Good -> StatusColors.delivered
    StatusTone.Waiting -> StatusColors.pending
    StatusTone.Bad -> StatusColors.refused
    StatusTone.Idle -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** Цвет состояния кассы: тот же в шапке окна и в карточке кассы кабинета. */
@Composable
fun kkmStateColor(blocked: Boolean, programming: Boolean): Color = toneColor(stateTone(blocked, programming))
