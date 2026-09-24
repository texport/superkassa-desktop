package kz.mybrain.superkassa.background

import android.content.Context
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.texport.superkassa.embedded.api.Superkassa
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kz.mybrain.superkassa.SuperkassaApp

/**
 * Фоновое дело кассы: один заход по всем кассам каталога.
 *
 * Пока касса на экране, досылку и автозакрытие ведёт сама касса процесса —
 * её корутины, — и заход ничего не делает: у кассы один писатель.
 * Без экрана Android будит процесс ради захода; касса процесса к этому
 * времени уже открывается в [SuperkassaApp], и заход берёт её же, а не
 * открывает вторую — замок каталога второй раз не берётся.
 *
 * Заход идёт через кассу процесса целиком, а не через фасад каждой кассы:
 * так досылка идёт под тем же замком писателя, что и чеки, и отказ одной
 * кассы каталога не останавливает остальные — это решает ядро.
 *
 * Касса не открылась или заход упал — он повторяется позже: без кассы
 * ни очереди, ни смен нет.
 */
abstract class KassaWork(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    /**
     * Заход по всем кассам каталога; вызов блокирующий.
     *
     * @return число отправленных документов или закрытых смен.
     */
    protected abstract fun serve(kassa: Superkassa): Int

    override suspend fun doWork(): Result {
        val opening = (applicationContext as SuperkassaApp).kassa
        return when {
            onScreen() -> Result.success().also { Log.i(TAG, "$name skipped: app is on screen") }
            !opened(opening) -> Result.retry().also { Log.w(TAG, "$name postponed: cash register did not open") }
            else -> pass(opening.await())
        }
    }

    private suspend fun opened(opening: Deferred<Superkassa>): Boolean {
        opening.join()
        return !opening.isCancelled
    }

    private suspend fun pass(kassa: Superkassa): Result {
        val served = withContext(Dispatchers.IO) { runCatching { serve(kassa) } }
        served.onSuccess { Log.i(TAG, "$name done: $it") }
            .onFailure { Log.w(TAG, "$name failed: ${it.javaClass.simpleName}") }
        return if (served.isSuccess) Result.success() else Result.retry()
    }

    private val name: String get() = javaClass.simpleName

    private fun onScreen(): Boolean =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private companion object {
        const val TAG = "KassaWork"
    }
}
