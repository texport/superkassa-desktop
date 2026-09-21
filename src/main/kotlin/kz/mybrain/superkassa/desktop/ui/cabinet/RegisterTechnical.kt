package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.server.cabinet.RegisterState
import kz.mybrain.superkassa.desktop.server.cabinet.TechnicalState
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.DetailLine
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.components.StatusTone
import kz.mybrain.superkassa.desktop.ui.components.toneColor
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Техническое состояние: ответ по существу, а под ним — кто его дал.
 *
 * Прежде карточка показывала показания трёх источников вперемешку
 * и одними «Да» и «Нет»: владелец видел «Смена · ОФД · Нет» и спрашивал,
 * что именно «нет» — закрыта смена или её не видно. Ответа на экране
 * не было вовсе, был набор голосований.
 *
 * Теперь сверху стоит один ответ словами — касса в работе, заблокирована
 * или не на учёте; смена открыта или закрыта, — а показания источников
 * идут под ним вторым планом: владельцу они нужны только при
 * расхождении. Расхождение названо строкой, а не одним цветом.
 *
 * Сведение и слова живут отдельно: `StateCheck` считает ответ,
 * `StateWords` называет его словами, здесь — только разметка.
 */
@Composable
fun RegisterTechnical(state: RegisterState?, texts: CabinetTexts, answers: List<StateAnswer>) {
    val technical = state?.technicalState
    answers.forEach { AnswerBlock(it, texts, technical) }
    TroubleRow(technical, texts)
    TechnicalFacts(technical, texts)
}

/**
 * Плашка в заголовке карточки и подсказка к ней.
 *
 * В заголовке стоит тот же ответ, что и внутри: прежде здесь был снимок
 * БФД, и свёрнутая карточка обещала «Работает» кассе, о которой узел
 * и кабинет говорили разное.
 */
@Composable
fun TechnicalHeader(texts: CabinetTexts, work: StateAnswer, disagree: Boolean) {
    InfoTip(texts.hints.technicalState)
    if (disagree) {
        Chip(texts.stateDisagree, toneColor(StatusTone.Bad))
    } else {
        Chip(texts.headlineWords(work.headline), toneColor(headlineTone(work.headline)))
    }
}

/** Ответ на один вопрос: сам ответ, под ним показания, под ними расхождение. */
@Composable
private fun AnswerBlock(answer: StateAnswer, texts: CabinetTexts, technical: TechnicalState?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
    ) {
        Text(
            text = texts.headlineWords(answer.headline),
            style = MaterialTheme.typography.titleMedium,
            color = toneColor(headlineTone(answer.headline))
        )
        ClaimRow(answer, texts, technical)
        DisagreeNote(answer, texts)
    }
}

/**
 * Показания источников — вторым планом.
 *
 * Молчащий источник стоит здесь же и говорит, что молчит: пропавшая
 * плашка читалась как отказ, а строка из одного источника рядом
 * со строкой из трёх — как дефект разметки.
 */
@Composable
private fun ClaimRow(answer: StateAnswer, texts: CabinetTexts, technical: TechnicalState?) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        answer.claims.forEach { claim ->
            Chip(
                text = texts.claimWords(claim, answer.question, technical),
                color = toneColor(claimTone(claim, answer))
            )
        }
    }
}

/**
 * Расхождение, названное словами.
 *
 * Прежде оно было видно только цветом плашек, и владелец читал красное
 * как поломку кассы. Сказано, кто с кем не согласен и что с этим делать:
 * почти всегда где-то состояние устарело.
 */
@Composable
private fun DisagreeNote(answer: StateAnswer, texts: CabinetTexts) {
    if (answer.disagreeing.isEmpty()) return
    val names = answer.disagreeing.joinToString(Glyphs.SEPARATOR) { it.title(texts) }
    Text(
        text = texts.stateDisagreeNote.format(names),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error
    )
}

/**
 * Цвет показания.
 *
 * Расходящееся — чинить, молчание — ожидание, а «об этом не высказываюсь
 * вовсе» — покой: кабинет смену не ведёт по устройству, и жёлтым владелец
 * читал это как ответ, которого ещё ждут.
 */
private fun claimTone(claim: StateClaim, answer: StateAnswer): StatusTone = when {
    claim.source in answer.disagreeing -> StatusTone.Bad
    silent(claim, answer.question) -> StatusTone.Idle
    answer.question.verdictOf(claim) == Verdict.Unknown -> StatusTone.Waiting
    else -> headlineTone(answer.headline)
}

/** Источник, которому этот вопрос не задают: у кабинета нет смен. */
private fun silent(claim: StateClaim, question: StateQuestion): Boolean =
    claim.source == StateSource.Cabinet && question == StateQuestion.Shift

/** Помехи, из-за которых документы перестают доезжать до БФД. */
@Composable
private fun TroubleRow(technical: TechnicalState?, texts: CabinetTexts) {
    val troubles = listOfNotNull(
        (texts.trafficSuspended to StatusTone.Bad).takeIf { technical?.trafficSuspended == true },
        (texts.bfdDisconnected to StatusTone.Waiting).takeIf { technical?.ofdDisconnected == true }
    )
    if (troubles.isEmpty()) return
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline)
    ) {
        troubles.forEach { (text, tone) -> Chip(text, toneColor(tone)) }
    }
}

/**
 * Сведения снимка БФД.
 *
 * Прочерка вместо значения здесь нет: «Последняя связь · —» владелец
 * читает как потерянные сведения и идёт искать поломку, а касса просто
 * ни разу не выходила на связь — так и написано. То же с номером смены:
 * его нет не потому, что сведения пропали, а потому, что смен не было.
 */
@Composable
private fun TechnicalFacts(technical: TechnicalState?, texts: CabinetTexts) {
    if (technical?.found != true) {
        DetailLine(texts.bfdSilenceTitle(technical), texts.bfdSilenceHint(technical))
        return
    }
    DetailLine(texts.shiftNumberTitle, technical.shiftNumber?.toString() ?: texts.shiftNumberNone)
    DetailLine(texts.lastContact, lastContactWords(technical, texts))
}

private fun lastContactWords(technical: TechnicalState, texts: CabinetTexts): String =
    technical.lastContactAt?.takeIf { it.isNotBlank() }?.let(::cabinetMoment) ?: texts.lastContactNever
