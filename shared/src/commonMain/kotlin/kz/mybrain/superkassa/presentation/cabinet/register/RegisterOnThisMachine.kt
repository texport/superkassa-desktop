package kz.mybrain.superkassa.presentation.cabinet.register

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
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kz.mybrain.superkassa.domain.kkm.model.displayName
import kz.mybrain.superkassa.domain.kkm.model.isBlocked
import kz.mybrain.superkassa.domain.kkm.model.isProgramming
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.register.adopt.AdoptRegisterDialog
import kz.mybrain.superkassa.presentation.cabinet.register.adopt.adoptViewModel
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.section.SubsectionTitle
import kz.mybrain.superkassa.presentation.common.status.Chip
import kz.mybrain.superkassa.presentation.common.status.kkmStateColor
import kz.mybrain.superkassa.presentation.shell.section.LocalSectionSwitch
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.presentation.strings.cabinet.CabinetTexts
import kz.mybrain.superkassa.presentation.strings.cabinet.MachineTexts
import kz.mybrain.superkassa.presentation.strings.cabinet.machineTexts
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Работает ли эта касса на этой машине.
 *
 * Владелец открывает паспорт кассы, чтобы понять, встанет ли за неё кассир
 * здесь и сейчас. Прежде ответа не было нигде: кассы кабинета и кассы узла
 * жили в разных разделах, и сверять их приходилось по идентификатору ОФД
 * глазами.
 *
 * Состояний ровно три — [NodeWork], — и действие есть только у одного.
 * Про соседние машины здесь не сказано ничего: касса этой машины о них не знает.
 */
@Composable
fun RegisterOnThisMachine(cabinet: CabinetWindow, texts: CabinetTexts, view: RegisterView) {
    val language = LocalLanguage.current
    var adopting by remember(view.row.id) { mutableStateOf(false) }
    val machine = machineTexts(language)
    val model = adoptViewModel(cabinet.cabinet)
    val adopt by model.state.collectAsScreenState()
    SubsectionTitle(machine.title, texts.hints.onThisMachine)
    // Пока касса этой машины не ответила списком, говорить «здесь не заведена» нельзя.
    if (!view.state.kkmsRead) return
    when (val work = nodeWork(view.card, view.state.kkms)) {
        is NodeWork.Here -> WorksHere(
            machine = machine,
            kkm = work.kkm,
            name = work.kkm.displayName(cabinet.cabinet.useCases.readLocalName(work.kkm.kkmId)),
            state = adopt.stateNames[work.kkm.state]?.let { words(it, language) } ?: work.kkm.state
        ) { model.workOn(work.kkm) }
        NodeWork.NotOnRecord -> Explanation(machine.onlyOnRecord)
        NodeWork.Absent -> AbsentHere(machine) { adopting = true }
    }
    if (adopting) {
        AdoptRegisterDialog(cabinet, texts, view) { adopting = false }
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
private fun WorksHere(machine: MachineTexts, kkm: KkmResponse, name: String, state: String, onWork: () -> Unit) {
    val switchTo = LocalSectionSwitch.current
    Explanation(machine.worksHere)
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalArrangement = Arrangement.spacedBy(Spacing.inline),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = name, style = MaterialTheme.typography.bodyMedium)
        Chip(text = state, color = kkmStateColor(kkm.isBlocked, kkm.isProgramming))
    }
    FilledTonalButton(onClick = {
        onWork()
        switchTo(Section.Dashboard)
    }) { Text(machine.goToKkm) }
}

/** Слова справочника кассы на языке владельца. */
private fun words(name: TrilingualMessageResponse, language: Language): String = when (language) {
    Language.Ru -> name.ru
    Language.Kk -> name.kk
    Language.En -> name.en
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
