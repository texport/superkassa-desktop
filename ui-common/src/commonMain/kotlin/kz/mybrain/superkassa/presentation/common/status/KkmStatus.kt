package kz.mybrain.superkassa.presentation.common.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmState
import kz.mybrain.superkassa.designsystem.status.Chip
import kz.mybrain.superkassa.designsystem.status.StatusTone
import kz.mybrain.superkassa.designsystem.status.toneColor
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.kkm.model.isAutonomous
import kz.mybrain.superkassa.domain.kkm.model.isBlocked
import kz.mybrain.superkassa.domain.kkm.model.isProgramming
import kz.mybrain.superkassa.strings.api.common.EnumTexts
import kz.mybrain.superkassa.strings.api.textsOf

/** Плашка шапки: слово и роль цвета. */
internal data class KkmStatusChip(val text: String, val tone: StatusTone)

/** Слова шапки, уже переведённые на язык кассира. */
internal data class KkmStatusWords(
    val state: String,
    val autonomous: String,
    val shiftOpen: String,
    val shiftClosed: String
)

/**
 * Набор плашек шапки по порядку.
 *
 * Состояние кассы названо ровно один раз — одним словом, а блокировка
 * остаётся цветом этой же плашки: своя плашка «Заблокирована» рядом
 * со словом «Заблокирована» давала кассиру одно и то же дважды подряд.
 *
 * Собирается без композиции, чтобы повтор ловился проверкой, а не глазами.
 */
internal fun kkmStatusChips(kkm: KkmResponse?, words: KkmStatusWords): List<KkmStatusChip> = listOfNotNull(
    kkm?.let { KkmStatusChip(words.state, stateTone(it.isBlocked, it.isProgramming)) },
    kkm?.takeIf { it.isAutonomous }?.let { KkmStatusChip(words.autonomous, StatusTone.Waiting) },
    kkm?.let { shiftChip(it.isShiftOpen, words) }
)

/**
 * Смена со слов кассы: касса отдаёт её состояние вместе с собой.
 *
 * Закрытая смена — ожидание, а не отказ: утром касса стоит именно так.
 */
private fun shiftChip(open: Boolean, words: KkmStatusWords): KkmStatusChip =
    if (open) KkmStatusChip(words.shiftOpen, StatusTone.Good) else KkmStatusChip(words.shiftClosed, StatusTone.Waiting)

/**
 * Название состояния кассы на языке кассира.
 *
 * Состояния называет ядро; незнакомое — новое в следующей версии ядра —
 * показывается своим кодом, а не пропадает: голая плашка без слова
 * значила бы «всё в порядке».
 */
internal fun EnumTexts.kkmState(code: String): String = when (KkmState.entries.firstOrNull { it.name == code }) {
    KkmState.ACTIVE -> stateActive
    KkmState.BLOCKED -> stateBlocked
    KkmState.PROGRAMMING -> stateProgramming
    KkmState.REGISTRATION -> stateRegistration
    null -> code
}

/**
 * Состояние кассы — одним набором плашек и в одном месте.
 *
 * Раньше состояние кассы стояло в настройках, режим программирования —
 * в диагностике, автономная работа — в шапке: кассир собирал картину
 * по трём экранам, а одно и то же слово в двух местах могло разойтись.
 * Набор объявлен здесь один раз и показывается там, где нужен, — в шапке
 * окна.
 *
 * Порядок от тревожного к обычному: состояние кассы и автономную работу
 * кассир обязан увидеть первыми.
 */
@Composable
fun KkmStatusChips(kkm: KkmResponse?) {
    val chips = kkmStatusChips(kkm, statusWords(kkm))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.inline),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        chips.forEach { chip -> Chip(chip.text, toneColor(chip.tone)) }
    }
}

/** Слова шапки на языке кассира. */
@Composable
private fun statusWords(kkm: KkmResponse?): KkmStatusWords {
    val texts = LocalStrings.current
    val core = textsOf(LocalLanguage.current).shift
    return KkmStatusWords(
        state = kkm?.let { texts.enums.kkmState(it.state) }.orEmpty(),
        autonomous = texts.topBar.autonomous,
        shiftOpen = core.shiftOpenShort,
        shiftClosed = core.shiftClosedShort
    )
}

/** Роль состояния кассы: блокировка — отказ, программирование — ожидание. */
internal fun stateTone(blocked: Boolean, programming: Boolean): StatusTone = when {
    blocked -> StatusTone.Bad
    programming -> StatusTone.Waiting
    else -> StatusTone.Good
}

/** Цвет состояния кассы: тот же в шапке окна и в карточке кассы кабинета. */
@Composable
fun kkmStateColor(blocked: Boolean, programming: Boolean): Color = toneColor(stateTone(blocked, programming))
