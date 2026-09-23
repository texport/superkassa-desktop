package kz.mybrain.superkassa.data.log

import android.util.Log
import kz.mybrain.superkassa.domain.journal.Journal

/** Журнал на Android — журнал системы под меткой кассы. */
class LogcatJournal : Journal {

    override fun info(text: String) {
        Log.i(TAG, text)
    }

    override fun warn(text: String) {
        Log.w(TAG, text)
    }

    override fun failure(text: String) {
        Log.e(TAG, text)
    }

    private companion object {
        const val TAG = "Superkassa"
    }
}
