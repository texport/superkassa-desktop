package kz.mybrain.superkassa.presentation.setup.registration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.presentation.common.model.latest
import kz.mybrain.superkassa.presentation.setup.CabinetCalls
import kotlin.time.Duration

/**
 * Постановка кассы на учёт: касса в кабинете, заявление и его подпись.
 *
 * Заявление готовит кабинет, подписывает владелец ключом ЭЦП, отправляет
 * снова кабинет; ответ КГД приходит не в ту же минуту. Обращения идут через
 * [calls]: помехи кабинета и подписи владелец видит словами кабинета окна.
 */
class RegistrationViewModel(
    private val cases: RegistrationCases,
    private val calls: CabinetCalls
) : ViewModel(), RegistrationActions {
    private val screen = MutableStateFlow(RegistrationUiState())
    private val submitting = latest()

    val state: StateFlow<RegistrationUiState> = screen.asStateFlow()

    override fun readRecord(registerId: String) {
        viewModelScope.launch { refresh(registerId) }
    }

    override suspend fun watchRecord(registerId: String, every: Duration) {
        while (!screen.value.onRecord(registerId)) {
            delay(every)
            refresh(registerId)
        }
    }

    /** Подача идёт одна: второе нажатие, пока владелец подписывает, ничего не начинает. */
    override fun submit(registerId: String) {
        submitting.startIfIdle {
            val sent = try {
                calls.run("submit registration") {
                    cases.submit(registerId) { on -> screen.update { it.copy(signing = on) } }
                }
            } finally {
                // Срок подписи снимается и с прерванной подачи.
                screen.update { it.copy(signing = false) }
            }
            if (sent != null) refresh(registerId)
        }
    }

    override fun cancelSubmit() = submitting.cancel()

    private suspend fun refresh(registerId: String) {
        val record = calls.run("read register") { cases.readRecord(registerId) } ?: return
        screen.update { it.copy(registerId = registerId, record = record) }
    }
}
