package kz.mybrain.superkassa.data.log

import android.util.Log
import kz.mybrain.superkassa.domain.debug.model.LogLevel
import kz.mybrain.superkassa.domain.log.port.Journal

/**
 * Журнал на Android — журнал системы под меткой кассы.
 *
 * Та же строка ложится в [book]: её видно в журнале поверх кассы, когда
 * включён режим отладки, — без кабеля и без средств разработчика.
 */
class LogcatJournal(private val book: LogcatBook) : Journal {

    override fun info(text: String) {
        Log.i(TAG, text)
        book.record(LogLevel.Info, text)
    }

    override fun warn(text: String) {
        Log.w(TAG, text)
        book.record(LogLevel.Warning, text)
    }

    override fun failure(text: String) {
        Log.e(TAG, text)
        book.record(LogLevel.Failure, text)
    }

    private companion object {
        const val TAG = "Superkassa"
    }
}
