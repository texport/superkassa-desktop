package kz.mybrain.superkassa.presentation.cabinet.applications

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.cabinet.model.ApplicationStage
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.presentation.cabinet.CabinetProblem
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.cabinet.isShiftOpen
import kz.mybrain.superkassa.presentation.cabinet.problem
import kz.mybrain.superkassa.presentation.cabinet.register.availableActions
import kz.mybrain.superkassa.presentation.cabinet.value
import kz.mybrain.superkassa.presentation.common.model.shown

/**
 * Заявление о выбранной кассе.
 *
 * @property stage на каком шаге подача; `null` — подача не идёт.
 * @property outcome чем кончилась последняя подача. Неудача остаётся под
 *   кнопкой до следующей подачи: всплывающая строка гаснет за секунды,
 *   и владелец не успевал прочитать причину.
 * @property closing открыт вопрос о закрытии смены перед снятием с учёта.
 * @property closingShift смена закрывается прямо сейчас.
 */
internal data class ApplicationUiState(
    val registerId: String? = null,
    val form: ApplicationForm = ApplicationForm(),
    val stage: ApplicationStage? = null,
    val outcome: ApplicationOutcome? = null,
    val closing: Boolean = false,
    val closingShift: Boolean = false
) {
    /** Кабинет отказал из-за открытой смены: главным становится действие, которое это чинит. */
    val blockedByShift: Boolean get() = (outcome as? ApplicationOutcome.Failed)?.problem?.isShiftOpen() == true
}

/**
 * Заявления в ИСНА: постановка на учёт, перерегистрация, снятие с учёта.
 *
 * Подача живёт в модели, а не на экране: владелец ждёт подписи до трёх
 * минут, и уход с карточки не должен обрывать поданное. Пока подача идёт,
 * вторая не начинается — занятость окна тут ни при чём: опрос ответа ИСНА
 * идёт своим чередом и подачу не отпускает.
 */
internal class ApplicationViewModel(private val cabinet: CabinetViewModel) : ViewModel() {
    private val screen = MutableStateFlow(ApplicationUiState())
    private var running: Job? = null

    /**
     * Итог и шаг подачи — у той кассы, чьё это заявление.
     *
     * Модель одна на окно, а владелец, пока ждёт подписи, переходит к другой
     * кассе. Прежде шаг и итог подачи писались в то, что на экране сейчас:
     * отсчёт подписи и «заявление отклонено» появлялись в карточке соседней
     * кассы, а у своей по возврату пропадали.
     */
    private val outcomes = mutableMapOf<String, ApplicationOutcome>()
    private var stages: Pair<String, ApplicationStage>? = null

    val state: StateFlow<ApplicationUiState> = screen.asStateFlow()

    /**
     * Карточка показывает кассу. Другая касса — чистое заявление; выбранным
     * остаётся только то, что по нынешнему состоянию кассы подаётся: иначе
     * владелец видел бы выбранным заявление, которое ИСНА отвергнет.
     */
    fun show(register: CabinetRegister) = screen.update { now ->
        val fresh = if (now.registerId == register.id) now else returnedTo(register.id)
        val available = availableActions(register)
        val kind = fresh.form.kind.takeIf { it in available } ?: available.firstOrNull() ?: fresh.form.kind
        fresh.copy(form = fresh.form.copy(kind = kind))
    }

    fun edit(form: ApplicationForm) = screen.update { it.copy(form = form) }

    /**
     * Подаёт заявление.
     *
     * @param here касса этой машины под этой кассой кабинета: перерегистрация
     *   меняет адрес в её чеках, и сведения ОФД в ней сверяются сразу.
     * @param onDone зовётся по окончании, удачном или нет: карточка перечитывается.
     */
    fun submit(register: CabinetRegister, here: KkmResponse?, onDone: () -> Unit) {
        if (running?.isActive == true) return
        running = viewModelScope.launch {
            send(register, here)
            onDone()
        }
    }

    /** Владелец прервал ожидание подписи: отменённая подача в кабинет ничего не отправляет. */
    fun cancel() {
        running?.cancel()
    }

    fun closing(open: Boolean) = screen.update { it.copy(closing = open) }

    /**
     * Закрывает смену кассы этой машины и подаёт то же заявление снова.
     *
     * Кабинет отказывает снять кассу с открытой сменой, а смену закрывает
     * касса — фискальной операцией и пином, который владелец набрал здесь.
     */
    fun closeShiftAndSubmit(pin: String, register: CabinetRegister, here: KkmResponse, onDone: () -> Unit) {
        if (running?.isActive == true) return
        screen.update { it.copy(closingShift = true) }
        running = viewModelScope.launch {
            val closed = cabinet.useCases.closeShift(here.kkmId, pin)
                .shown(cabinet.texts.closeShiftAndDeregister, "close shift before deregistration", cabinet.talk)
            screen.update { it.copy(closingShift = false, closing = false) }
            if (closed != null) send(register, here)
            onDone()
        }
    }

    private suspend fun send(register: CabinetRegister, here: KkmResponse?) {
        val id = register.id
        val form = screen.value.form
        outcomes.remove(id)
        on(id) { it.copy(outcome = null) }
        val reply = try {
            cabinet.work.run("submit application") {
                cabinet.useCases.submitApplication(form.application(id)) { stage ->
                    stages = id to stage
                    on(id) { it.copy(stage = stage) }
                }
            }
        } finally {
            // Отсчёт снимается и с отменённой подачи: ждать его уже некому.
            stages = null
            on(id) { it.copy(stage = null) }
        }
        val outcome = reply.value?.let(ApplicationOutcome::Sent)
            ?: ApplicationOutcome.Failed(reply.problem ?: CabinetProblem.SignDeclined(""))
        outcomes[id] = outcome
        on(id) { it.copy(outcome = outcome) }
        if (form.kind == ActionKind.Reregistration && outcome is ApplicationOutcome.Sent) syncLocal(here)
    }

    /** Меняет состояние, только если на экране касса [id]. */
    private fun on(id: String, change: (ApplicationUiState) -> ApplicationUiState) =
        screen.update { if (it.registerId == id) change(it) else it }

    /** Касса, к которой вернулись: её итог и шаг идущей подачи, если он её. */
    private fun returnedTo(id: String) = ApplicationUiState(
        registerId = id,
        outcome = outcomes[id],
        stage = stages?.takeIf { it.first == id }?.second
    )

    /**
     * Обновляет сведения кассы этой машины после перерегистрации.
     *
     * Перерегистрация меняет торговую точку, а её адрес печатается в чеке:
     * касса о переезде не знает, пока не сверится с БФД. Сверку касса делает
     * только по закрытой смене — счётчики открытой смены сверять нельзя, —
     * поэтому отказ здесь не помеха, а повод сказать, когда адрес дойдёт до чека.
     */
    private suspend fun syncLocal(here: KkmResponse?) {
        val talk = cabinet.talk
        val synced = cabinet.useCases.syncServiceInfo(here) ?: return
        if (synced !is Answer.Done) talk.journal.warn("service info not synced after reregistration")
        val texts = cabinet.texts
        talk.done(if (synced is Answer.Done) texts.localInfoSynced else texts.localInfoNeedsSync)
    }
}

/** Модель заявлений окна: подача переживает переход между вкладками кассы. */
@Composable
internal fun applicationViewModel(cabinet: CabinetViewModel): ApplicationViewModel =
    viewModel { ApplicationViewModel(cabinet) }
