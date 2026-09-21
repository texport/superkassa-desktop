package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.titleOf
import kz.mybrain.superkassa.desktop.app.workOn
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.RegisterState
import kz.mybrain.superkassa.desktop.ui.LocalSectionSwitch
import kz.mybrain.superkassa.desktop.ui.Section
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.SubsectionTitle
import kz.mybrain.superkassa.desktop.ui.components.kkmStateColor
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.MachineTexts
import kz.mybrain.superkassa.desktop.ui.strings.machineTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Работает ли эта касса на этой машине.
 *
 * Владелец открывает паспорт кассы, чтобы понять, встанет ли за неё кассир
 * здесь и сейчас. Прежде ответа не было нигде: кассы кабинета и кассы узла
 * жили в разных разделах, и сверять их приходилось по идентификатору ОФД
 * глазами.
 *
 * Состояний ровно три — [NodeWork], — и действие есть только у одного.
 * Про соседние машины здесь не сказано ничего: узел о них не знает.
 */
@Composable
fun RegisterOnThisMachine(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    register: CabinetRegister,
    state: RegisterState?
) {
    var adopting by remember(register.id) { mutableStateOf(false) }
    val machine = machineTexts(session.language)

    SubsectionTitle(machine.title, texts.onThisMachineHint)
    when (val work = nodeWork(register, session.kkms)) {
        is NodeWork.Here -> WorksHere(session, machine, work.kkm)
        NodeWork.NotOnRecord -> Explanation(machine.onlyOnRecord)
        NodeWork.Absent -> AbsentHere(machine) { adopting = true }
    }
    if (adopting) {
        AdoptRegisterDialog(session, cabinet, texts, register, state) { adopting = false }
    }
}

/**
 * Касса заведена здесь: её местное название, состояние и переход к ней.
 *
 * Переход уводит на вход по пину: пин принадлежит кассе, и чужой к этой
 * не подойдёт. Кнопки заведения в этом состоянии нет — заводить нечего.
 *
 * Уже выбранная касса вход не повторяет, поэтому одного выбора кассы мало:
 * владелец оставался на паспорте в кабинете и решал, что кнопка не работает.
 * Раздел меняется здесь же — переход и есть уход из кабинета на кассу.
 */
@Composable
private fun WorksHere(session: Session, machine: MachineTexts, kkm: Kkm) {
    val switchTo = LocalSectionSwitch.current
    Explanation(machine.worksHere)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = session.displayName(kkm), style = MaterialTheme.typography.bodyMedium)
        Chip(
            text = session.titleOf(Dictionary.KkmStates, kkm.state),
            color = kkmStateColor(kkm.isBlocked, kkm.isProgramming)
        )
    }
    FilledTonalButton(onClick = {
        session.workOn(kkm)
        switchTo(Section.Dashboard)
    }) { Text(machine.goToKkm) }
}

/** Касса на учёте, а здесь не заведена — единственное состояние с действием. */
@Composable
private fun AbsentHere(machine: MachineTexts, onStart: () -> Unit) {
    Explanation(machine.notHere)
    FilledTonalButton(onClick = onStart) { Text(machine.workHere) }
}

/** Объяснение состояния словами: кодов состояний на этом экране нет. */
@Composable
private fun Explanation(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
