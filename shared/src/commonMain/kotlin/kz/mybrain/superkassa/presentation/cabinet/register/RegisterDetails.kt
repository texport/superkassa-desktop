package kz.mybrain.superkassa.presentation.cabinet.register

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.documents.RegistrationAction
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.cabinet.CabinetWindow
import kz.mybrain.superkassa.presentation.cabinet.applications.RegistrationActionsBlock
import kz.mybrain.superkassa.presentation.cabinet.documents.RegisterDocumentsCard
import kz.mybrain.superkassa.presentation.cabinet.register.card.RegistrationCardBlock
import kz.mybrain.superkassa.presentation.cabinet.register.component.RegisterTechnical
import kz.mybrain.superkassa.presentation.cabinet.register.component.StateQuestion
import kz.mybrain.superkassa.presentation.cabinet.register.component.TechnicalHeader
import kz.mybrain.superkassa.presentation.cabinet.register.component.disagreeing
import kz.mybrain.superkassa.presentation.cabinet.register.component.stateAnswers
import kz.mybrain.superkassa.presentation.cabinet.register.component.stateClaims
import kz.mybrain.superkassa.presentation.common.list.ScrollableColumn
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.section.CollapsibleCard
import kz.mybrain.superkassa.presentation.strings.cabinet.CabinetTexts
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/**
 * Выбранная касса: чем она является, как себя чувствует и что с ней делали.
 *
 * Карточка была лентой из семи одинаковых карточек в полтора экрана
 * прокрутки: паспорт, правка, состояние, регистрационная карта, заявления,
 * токен, журнал — и всё это открыто одновременно. Теперь развёрнуто то,
 * ради чего кассу открывают: состояние и заявления в ИСНА. Регистрационная
 * карта и журнал остаются заголовками и раскрываются по нажатию.
 *
 * Паспорт не сворачивается: это ответ на вопрос «какая это касса»,
 * и без него остальные разделы теряют предмет. Правка реквизитов и выдача
 * токена живут в нём же — своих разделов у них больше нет.
 */
@Composable
fun RegisterDetails(
    cabinet: CabinetWindow,
    texts: CabinetTexts,
    register: CabinetRegister,
    modifier: Modifier = Modifier
) {
    val language = LocalLanguage.current
    val model = registerViewModel(cabinet.cabinet)
    val state by model.state.collectAsScreenState()
    // Касса на экране — модель её читает и, пока ИСНА рассматривает
    // заявление, опрашивает; ушла с экрана — опрашивать некого.
    LaunchedEffect(register) { model.show(register) }
    DisposableEffect(Unit) { onDispose { model.hide() } }
    // Прочитанная карточка подменяет собой строку списка: в ней есть признак
    // регистрационной карты и последнее действие, которых в строке нет.
    val card = state.card?.takeIf { it.id == register.id } ?: register
    val view = RegisterView(register, card, state)
    ScrollableColumn(modifier = modifier.fillMaxWidth(), spacing = Spacing.snug) {
        RegisterPassport(cabinet, texts, view, model)
        RegisterLiveBlocks(cabinet.cabinet, language, texts, view, model)
        // Документы кассы живут своим экраном: в карточке остаётся переход
        // к ним, а список с поиском, отбором и печатью открывается во всю
        // ширину рабочего места.
        RegisterDocumentsCard(language, register)
        RegisterAdminBlocks(texts, state.actions, state.open, model::toggle)
    }
}

/**
 * Касса, какой её показывает карточка: строка списка, прочитанная карточка
 * и всё, что о ней известно.
 */
data class RegisterView(val row: CabinetRegister, val card: CabinetRegister, val state: RegisterUiState)

/** То, ради чего кассу открывают: как она себя чувствует и что с ней подано. */
@Composable
private fun RegisterLiveBlocks(
    cabinet: CabinetViewModel,
    language: Language,
    texts: CabinetTexts,
    view: RegisterView,
    model: RegisterViewModel
) {
    val state = view.state
    // Показания источников считаются один раз: плашка в шапке карточки
    // и сами показания внутри обязаны говорить об одном.
    val claims = stateClaims(state.here, view.row, state.state?.technicalState)
    val answers = stateAnswers(claims)
    val work = answers.first { it.question == StateQuestion.Usable }
    RegisterBlockCard(
        block = RegisterBlock.Technical,
        open = state.open,
        onToggle = model::toggle,
        title = texts.technicalState,
        trailing = { TechnicalHeader(texts, work, disagreeing(claims).isNotEmpty()) }
    ) {
        RegisterTechnical(state.state, texts, answers)
    }
    RegisterBlockCard(RegisterBlock.Applications, state.open, model::toggle, texts.applications) {
        RegistrationActionsBlock(cabinet, language, texts, view, model::reload)
    }
    RegisterBlockCard(RegisterBlock.Card, state.open, model::toggle, texts.card, info = texts.hints.card) {
        RegistrationCardBlock(cabinet, texts, view.card)
    }
}

/** След регистрационных действий: к нему возвращаются редко. */
@Composable
private fun RegisterAdminBlocks(
    texts: CabinetTexts,
    actions: List<RegistrationAction>,
    open: Set<RegisterBlock>,
    onToggle: (RegisterBlock) -> Unit
) {
    RegisterBlockCard(
        block = RegisterBlock.Journal,
        open = open,
        onToggle = onToggle,
        title = texts.actionsJournal,
        info = texts.hints.actionsJournal,
        trailing = { ActionsCount(actions.size) }
    ) {
        RegisterJournal(actions, texts)
    }
}

/**
 * Раздел карточки кассы: заголовок со стрелкой, объяснение и содержимое.
 *
 * Свёрнутый раздел — одна строка названия, и «Регистрационную карту»
 * раскрывали только затем, чтобы узнать, что там лежит.
 */
@Composable
private fun RegisterBlockCard(
    block: RegisterBlock,
    open: Set<RegisterBlock>,
    onToggle: (RegisterBlock) -> Unit,
    title: String,
    info: String? = null,
    trailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    CollapsibleCard(
        title = title,
        expanded = block in open,
        onToggle = { onToggle(block) },
        info = info,
        trailing = trailing,
        content = content
    )
}
