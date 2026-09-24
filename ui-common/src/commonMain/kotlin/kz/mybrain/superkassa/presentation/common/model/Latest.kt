package kz.mybrain.superkassa.presentation.common.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Одна работа за раз: новая отменяет идущую.
 *
 * Так читается касса: чтение прежней кассы, пришедшее после смены кассира,
 * записало бы чужую смену в экран нового. Отменённое чтение ничего
 * не пишет, а новое начинается сразу.
 */
class Latest(private val scope: CoroutineScope) {
    private var job: Job? = null

    /** Идёт ли работа сейчас. */
    val running: Boolean get() = job?.isActive == true

    /** Отменяет идущую работу и начинает [work]. */
    fun restart(work: suspend () -> Unit): Job {
        job?.cancel()
        return scope.launch { work() }.also { job = it }
    }

    /** Начинает [work], только если ничего не идёт: повторное нажатие не начинает второе чтение. */
    fun startIfIdle(work: suspend () -> Unit): Job? = if (running) null else restart(work)

    /** Отменяет идущую работу: касса сменилась, и читать больше нечего. */
    fun cancel() {
        job?.cancel()
    }
}

/** Одна работа за раз в пределах жизни модели. */
fun ViewModel.latest(): Latest = Latest(viewModelScope)
