package kz.mybrain.superkassa.background

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Расписание фоновой работы кассы.
 *
 * Оба дела — периодические, с наименьшим периодом, который допускает
 * Android. Расписание уникально по имени и обновляется, а не множится,
 * при каждом запуске приложения; после перезагрузки его возвращает
 * WorkManager.
 */
object BackgroundWork {
    private const val QUEUE = "kassa.queue"
    private const val SHIFT = "kassa.shift"

    fun schedule(context: Context) {
        val work = WorkManager.getInstance(context)
        val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        work.enqueueUniquePeriodicWork(QUEUE, ExistingPeriodicWorkPolicy.UPDATE, periodic<QueueWork>(online))
        work.enqueueUniquePeriodicWork(SHIFT, ExistingPeriodicWorkPolicy.UPDATE, periodic<ShiftWork>(Constraints.NONE))
    }

    private inline fun <reified W : KassaWork> periodic(constraints: Constraints): PeriodicWorkRequest =
        PeriodicWorkRequestBuilder<W>(PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()
}
