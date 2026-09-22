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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
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
    session: Session,
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

    // Прочитанная карточка подменяет собой строку списка: статус в дереве
    // слева иначе оставался «черновиком» у кассы, только что поставленной
    // на учёт. Меняется одна строка, а не перечитывается список компании:
    // у сети он в сорока страницах, а карточка опрашивается каждые
    // несколько секунд, пока ИСНА не ответит.
    suspend fun reload() {
        val token = cabinet.token ?: return
        card = cabinet.guard { cabinet.client.register(token, register.id) } ?: register
        state = cabinet.guard { cabinet.client.registerState(token, register.id) }
        actions = cabinet.guard { cabinet.client.registrationActions(token, register.id) }?.items.orEmpty()
        cabinet.registerChanged(card)
    }

    LaunchedEffect(register.id, cabinet.token) { reload() }

    // Пока заявление в ИСНА, карточка перечитывается сама: ответ приходит
    // через десятки секунд, и подпись под кнопками обещает владельцу, что
    // журнал обновится без него. Без этого касса оставалась «в обработке»
    // и снятой с обслуживания, пока раздел не открывали заново.
    //
    // Ожидание читается и по карточке, и по строке списка: пока заявление
    // в работе, запрос карточки может и не удаться, и одной карточки мало —
    // со снятием с учёта опрос так и не начинался.
    LaunchedEffect(card.status, register.status) {
        while (awaitingIsna(card) || awaitingIsna(register)) {
            delay(ISNA_ANSWER_POLL_MS)
            reload()
        }
    }

    val toggle: (RegisterBlock) -> Unit = { block ->
        open = if (block in open) open - block else open + block
    }
    ScrollableColumn(modifier = modifier.fillMaxWidth(), spacing = Spacing.snug) {
        RegisterPassport(session, cabinet, texts, card, state) { scope.launch { reload() } }
        RegisterLiveBlocks(session, cabinet, texts, register, card, state, open, toggle) {
            scope.launch { reload() }
        }
        // Документы кассы живут своим экраном: в карточке остаётся переход
        // к ним, а список с поиском, отбором и печатью открывается во всю
        // ширину рабочего места.
        RegisterDocumentsCard(session.language, register)
        RegisterAdminBlocks(texts, actions, open, toggle)
    }
}

/** То, ради чего кассу открывают: как она себя чувствует и что с ней подано. */
@Composable
private fun RegisterLiveBlocks(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    register: CabinetRegister,
    card: CabinetRegister,
    state: RegisterState?,
    open: Set<RegisterBlock>,
    onToggle: (RegisterBlock) -> Unit,
    onDone: () -> Unit
) {
    // Показания источников считаются один раз: плашка в шапке карточки
    // и сами показания внутри обязаны говорить об одном.
    val claims = stateClaims(
        kkm = closableHere(register, session.kkms),
        register = register,
        technical = state?.technicalState,
        shift = session.shiftState
    )
    val answers = stateAnswers(claims)
    val work = answers.first { it.question == StateQuestion.Usable }
    RegisterBlockCard(
        block = RegisterBlock.Technical,
        open = open,
        onToggle = onToggle,
        title = texts.technicalState,
        trailing = { TechnicalHeader(texts, work, disagreeing(claims).isNotEmpty()) }
    ) {
        RegisterTechnical(state, texts, answers)
    }
    RegisterBlockCard(RegisterBlock.Applications, open, onToggle, texts.applications) {
        RegistrationActionsBlock(session, cabinet, texts, register, onDone)
    }
    RegisterBlockCard(RegisterBlock.Card, open, onToggle, texts.card, info = texts.hints.card) {
        RegistrationCardBlock(cabinet, texts, card)
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

/** Разделы карточки кассы. */
enum class RegisterBlock { Technical, Applications, Card, Journal }

/**
 * Что раскрыто при открытии кассы.
 *
 * Состояние и заявления: за ними в карточку и приходят. Остальное —
 * заголовками, чтобы карточка помещалась на экран целиком.
 */
private val OPENED_AT_START = setOf(RegisterBlock.Technical, RegisterBlock.Applications)

/** Как часто спрашивать кабинет об ответе ИСНА: он приходит через десятки секунд. */
private const val ISNA_ANSWER_POLL_MS = 5_000L
