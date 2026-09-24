package kz.mybrain.superkassa.presentation.common.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Занятость модели: сколько работ, начатых владельцем, идёт сейчас.
 *
 * Счётчик, а не флаг: пока идёт подача заявления, карточка кассы опрашивает
 * кассу, и первое же завершившееся обращение гасило флаг посреди подписи —
 * кнопка снова становилась нажимаемой.
 */
class Busy {
    private val count = MutableStateFlow(0)

    /** Сколько работ идёт сейчас. */
    val running: StateFlow<Int> = count.asStateFlow()

    /** Занята ли модель — для состояния экрана: кнопки гаснут, полоска ожидания видна. */
    val active: Flow<Boolean> = count.map { it > 0 }.distinctUntilChanged()

    /** Занята ли модель сейчас, без подписки. */
    val now: Boolean get() = count.value > 0

    /** Выполняет [block], занимая модель на время работы. */
    suspend fun <T> during(block: suspend () -> T): T {
        count.update { it + 1 }
        try {
            return block()
        } finally {
            count.update { it - 1 }
        }
    }

    internal fun enter() = count.update { it + 1 }

    internal fun leave() = count.update { it - 1 }
}

/**
 * Работа владельца, на время которой кнопки гаснут.
 *
 * Касса выполняет команду секунду-другую, и второе нажатие отправляло бы
 * ей то же самое второй раз: пока идёт одна работа, вторая не начинается.
 * Занятость отмечается до запуска, а не внутри него: два быстрых нажатия
 * проходили проверку оба, пока первая работа ждала очереди.
 *
 * @return начатая работа; `null` — модель занята, и ничего не начато.
 */
fun ViewModel.whileBusy(busy: Busy, work: suspend () -> Unit): Job? {
    if (busy.now) return null
    busy.enter()
    return viewModelScope.launch { work() }.apply { invokeOnCompletion { busy.leave() } }
}
