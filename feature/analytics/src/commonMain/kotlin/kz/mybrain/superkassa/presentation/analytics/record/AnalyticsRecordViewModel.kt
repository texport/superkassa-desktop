package kz.mybrain.superkassa.presentation.analytics.record

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kz.mybrain.superkassa.domain.analytics.model.KkmMapView
import kz.mybrain.superkassa.presentation.analytics.common.OwnerAccess
import kz.mybrain.superkassa.presentation.analytics.common.Reading
import kz.mybrain.superkassa.presentation.common.model.latest

/**
 * Вкладка учёта: кассы компании и то, как КГД их учёл.
 *
 * Модель живёт, пока открыто окно: вернувшись на вкладку, владелец
 * видит прочитанное, а не ждёт его снова.
 */
internal class AnalyticsRecordViewModel(private val cases: RecordCases) : ViewModel() {
    private val screen = MutableStateFlow(Reading<KkmMapView>())
    private val access = OwnerAccess()
    private val reading = latest()

    val state: StateFlow<Reading<KkmMapView>> = screen.asStateFlow()

    /** Вошёл другой владелец или вышел этот: прежний ответ не его. */
    fun follow(owner: String?) {
        if (!access.changed(owner)) return
        screen.value = Reading()
        refresh()
    }

    /** Спрашивает кабинет о кассах компании заново. */
    fun refresh() {
        if (access.current == null) return
        reading.restart {
            screen.update { it.started() }
            screen.value = Reading.of(cases.read())
        }
    }
}
