package kz.mybrain.superkassa.data.log

import android.util.Log
import kz.mybrain.superkassa.domain.log.port.Journal

/**
 * Журнал на Android — журнал системы под меткой кассы и журнал рабочего
 * места, как на компьютере.
 *
 * Та же строка ложится в [AppLog]: её видно в журнале поверх кассы, когда
 * включён режим отладки, — без кабеля и без средств разработчика, — и она
 * остаётся в файле, который пересылают в поддержку.
 */
class LogcatJournal(private val journal: Journal = AppJournal(LogSource.App)) : Journal {

    override fun info(text: String) {
        Log.i(TAG, text)
        journal.info(text)
    }

    override fun warn(text: String) {
        Log.w(TAG, text)
        journal.warn(text)
    }

    override fun failure(text: String) {
        Log.e(TAG, text)
        journal.failure(text)
    }

    private companion object {
        const val TAG = "Superkassa"
    }
}
