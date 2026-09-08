package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.RegisterState
import kz.mybrain.superkassa.desktop.server.cabinet.RegistrationAction
import kz.mybrain.superkassa.desktop.server.cabinet.register
import kz.mybrain.superkassa.desktop.server.cabinet.registerState
import kz.mybrain.superkassa.desktop.server.cabinet.registrationActions
import kz.mybrain.superkassa.desktop.ui.components.CollapsibleCard
import kz.mybrain.superkassa.desktop.ui.components.ScrollableColumn
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

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
    cabinet: CabinetSession,
    texts: CabinetTexts,
    register: CabinetRegister,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var state by remember(register.id) { mutableStateOf<RegisterState?>(null) }
    var actions by remember(register.id) { mutableStateOf<List<RegistrationAction>>(emptyList()) }
    // Список отдаёт краткую запись кассы, а признак регистрационной карты
    // и последнее действие есть только в её карточке: без этого запроса
    // блок карты не открылся бы ни у одной кассы.
    var card by remember(register.id) { mutableStateOf(register) }
    var open by remember(register.id) { mutableStateOf(OPENED_AT_START) }

    suspend fun reload() {
        val token = cabinet.token ?: return
        card = cabinet.guard { cabinet.client.register(token, register.id) } ?: register
        state = cabinet.guard { cabinet.client.registerState(token, register.id) }
        actions = cabinet.guard { cabinet.client.registrationActions(token, register.id) }?.items.orEmpty()
    }

    LaunchedEffect(register.id, cabinet.token) { reload() }

    val toggle: (RegisterBlock) -> Unit = { block ->
        open = if (block in open) open - block else open + block
    }
    ScrollableColumn(modifier = modifier.fillMaxWidth(), spacing = Spacing.snug) {
        RegisterPassport(cabinet, texts, card) { scope.launch { reload() } }
        RegisterLiveBlocks(cabinet, texts, register, card, state, open, toggle) {
            scope.launch { reload() }
        }
        RegisterAdminBlocks(texts, actions, open, toggle)
    }
}

/** То, ради чего кассу открывают: как она себя чувствует и что с ней подано. */
@Composable
private fun RegisterLiveBlocks(
    cabinet: CabinetSession,
    texts: CabinetTexts,
    register: CabinetRegister,
    card: CabinetRegister,
    state: RegisterState?,
    open: Set<RegisterBlock>,
    onToggle: (RegisterBlock) -> Unit,
    onDone: () -> Unit
) {
    RegisterBlockCard(
        block = RegisterBlock.Technical,
        open = open,
        onToggle = onToggle,
        title = texts.technicalState,
        trailing = { CabinetStatusChip(state?.technicalState?.status, texts) }
    ) {
        RegisterTechnical(state, texts)
    }
    RegisterBlockCard(RegisterBlock.Applications, open, onToggle, texts.applications) {
        RegistrationActionsBlock(cabinet, texts, register, onDone)
    }
    RegisterBlockCard(RegisterBlock.Card, open, onToggle, texts.card) {
        RegistrationCardBlock(cabinet, texts, card)
    }
    RegisterBlockCard(RegisterBlock.Documents, open, onToggle, texts.documents) {
        RegisterDocuments(cabinet, texts, register.id)
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
        trailing = { ActionsCount(actions.size) }
    ) {
        RegisterJournal(actions, texts)
    }
}

/** Раздел карточки кассы: заголовок со стрелкой и содержимое под ним. */
@Composable
private fun RegisterBlockCard(
    block: RegisterBlock,
    open: Set<RegisterBlock>,
    onToggle: (RegisterBlock) -> Unit,
    title: String,
    trailing: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    CollapsibleCard(
        title = title,
        expanded = block in open,
        onToggle = { onToggle(block) },
        trailing = trailing,
        content = content
    )
}

/** Разделы карточки кассы. */
enum class RegisterBlock { Technical, Applications, Card, Documents, Journal }

/**
 * Что раскрыто при открытии кассы.
 *
 * Состояние и заявления: за ними в карточку и приходят. Остальное —
 * заголовками, чтобы карточка помещалась на экран целиком.
 */
private val OPENED_AT_START = setOf(RegisterBlock.Technical, RegisterBlock.Applications)
