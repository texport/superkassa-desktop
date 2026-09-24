package kz.mybrain.superkassa.background

import android.content.Context
import androidx.work.WorkerParameters
import io.github.texport.superkassa.embedded.api.Superkassa

/**
 * Автозакрытие смены без экрана.
 *
 * Пора ли закрывать смену и что делать при отказе, решает ядро: закрывает
 * только у касс с настройкой «автозакрытие» и только у предела в сутки.
 * Сеть не требуется: без неё Z-отчёт ложится в автономную очередь.
 */
class ShiftWork(context: Context, params: WorkerParameters) : KassaWork(context, params) {
    override fun serve(kassa: Superkassa): Int = kassa.closeDueShiftsNow()
}
