package kz.mybrain.superkassa.background

import android.content.Context
import androidx.work.WorkerParameters
import io.github.texport.superkassa.embedded.api.Superkassa

/**
 * Досылка автономной очереди в БФД без экрана.
 *
 * Запускается, только когда есть сеть: без неё заход ничего бы не отправил.
 * Порядок документов, размер захода и повторы решает ядро; досылка идёт
 * под замком писателя кассы и не встаёт параллельно с новым чеком.
 */
class QueueWork(context: Context, params: WorkerParameters) : KassaWork(context, params) {
    override fun serve(kassa: Superkassa): Int = kassa.sendQueueNow()
}
