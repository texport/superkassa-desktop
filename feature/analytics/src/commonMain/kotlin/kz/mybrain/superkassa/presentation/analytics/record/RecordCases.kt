package kz.mybrain.superkassa.presentation.analytics.record

import kz.mybrain.superkassa.domain.analytics.port.Analytics
import kz.mybrain.superkassa.domain.analytics.usecase.ReadRecord

/** Сценарии вкладки учёта. */
internal class RecordCases(analytics: Analytics) {
    val read = ReadRecord(analytics)
}
