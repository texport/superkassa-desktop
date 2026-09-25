package kz.mybrain.superkassa.presentation.cabinet.register

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.presentation.cabinet.CabinetReply
import kz.mybrain.superkassa.presentation.cabinet.CabinetViewModel
import kz.mybrain.superkassa.presentation.cabinet.value
import kotlin.time.Duration.Companion.seconds

/**
 * Выбранная касса кабинета: карточка, состояние и правка реквизитов.
 *
 * Пока заявление в ИСНА, карточка перечитывается сама: ответ приходит
 * через десятки секунд, и подпись под кнопками обещает владельцу, что
 * журнал обновится без него. Перечитывание молчаливое: оно не занимает
 * окно, не пишет в строку сообщений и не стирает показанный там отказ —
 * владелец не просил его, и прочитанное им не должно пропадать.
 */
internal class RegisterViewModel(private val cabinet: CabinetViewModel) : ViewModel() {
    private val screen = MutableStateFlow(RegisterUiState())
    private val cases = cabinet.useCases
    private val reader = RegisterReader(cabinet)
    private var shown: CabinetRegister? = null
    private var polling: Job? = null

    val state: StateFlow<RegisterUiState> = screen.asStateFlow()

    /**
     * Открывает кассу: читает её карточку, состояние, заявления и кассы машины.
     * Та же касса повторно не перечитывается — только опрос проверяется.
     */
    fun show(register: CabinetRegister) {
        val same = shown?.id == register.id
        shown = register
        if (!same) {
            screen.value = RegisterUiState(card = register)
            viewModelScope.launch { read(register, loud = true) }
        }
        poll()
    }

    /** Карточка ушла с экрана: опрашивать некого. */
    fun hide() {
        polling?.cancel()
    }

    fun toggle(block: RegisterBlock) = screen.update {
        it.copy(open = if (block in it.open) it.open - block else it.open + block)
    }

    /** Перечитывает кассу по просьбе владельца или после его действия. */
    fun reload() {
        val register = shown ?: return
        viewModelScope.launch { read(register, loud = true) }
    }

    /** Своё название владельца: заметка для него, в ОФД не уходит; пустое кабинет не примет. */
    fun rename(value: String) = change("rename register") { id -> cases.renameRegister(id, value) }

    /** Заводской номер: правится, пока касса не поставлена на учёт. */
    fun restamp(value: String) = change("edit register") { cases.restampRegister(it, value) }

    /** Удаляет черновик кассы: поставленную удаляют заявлением. */
    fun remove() {
        val id = shown?.id ?: return
        viewModelScope.launch {
            if (cabinet.work.run("remove register") { cases.removeRegister(id) } is CabinetReply.Done) cabinet.reload()
        }
    }

    /**
     * Правка кассы: исправленная карточка подменяет строку списка — статус
     * в дереве иначе оставался прежним у только что изменённой кассы.
     */
    private fun change(action: String, request: suspend (String) -> CabinetRegister?) {
        val id = shown?.id ?: return
        viewModelScope.launch {
            val changed = cabinet.work.run(action) { request(id) }.value ?: return@launch
            shown = changed
            screen.update { it.copy(card = changed) }
            cabinet.registerChanged(changed)
        }
    }

    /**
     * Опрос, пока заявление в ИСНА. Ожидание читается и по карточке,
     * и по строке списка: пока заявление в работе, запрос карточки может
     * и не удаться, и одной карточки мало.
     */
    private fun poll() {
        polling?.cancel()
        polling = viewModelScope.launch {
            while (isActive && listOfNotNull(shown, screen.value.card).any(::awaitingIsna)) {
                delay(ISNA_ANSWER_POLL)
                shown?.let { read(it, loud = false) }
            }
        }
    }

    private suspend fun read(register: CabinetRegister, loud: Boolean) {
        val read = reader.read(register.id, loud)
        if (shown?.id != register.id) return
        screen.update {
            it.copy(card = read.card ?: it.card, state = read.state ?: it.state, actions = read.actions ?: it.actions)
        }
        read.card?.let(cabinet::registerChanged)
        read.kkms?.let { kkms -> screen.update { it.copy(kkms = kkms, kkmsRead = true) } }
        if (polling?.isActive != true) poll()
    }

    private companion object {
        /** Как часто спрашивать кабинет об ответе ИСНА: он приходит через десятки секунд. */
        val ISNA_ANSWER_POLL = 5.seconds
    }
}

/** Модель выбранной кассы окна: одна, и следует за выбором. */
@Composable
internal fun registerViewModel(cabinet: CabinetViewModel): RegisterViewModel = viewModel { RegisterViewModel(cabinet) }
