package kz.mybrain.superkassa.presentation.settings.ofd

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.settings.model.KkmSettingRules
import kz.mybrain.superkassa.domain.settings.model.OfdSummary

/**
 * Связь кассы с БФД: сверка, токен и диагностика.
 *
 * @property token набранный новый токен ОФД; уходит в кассу и не хранится.
 * @property linkAlive ответил ли ОФД на последнюю проверку; `null` — не проверяли.
 * @property summary сведения ОФД о кассе, сведённые к тому, что читает кассир.
 * @property nextRequest номер следующего запроса кассы к БФД; `null` — не спрашивали.
 * @property admin за кассой администратор: номер запроса касса отдаёт только ему.
 */
data class OfdSettingsUiState(
    val kkm: KkmResponse? = null,
    val token: String = "",
    val linkAlive: Boolean? = null,
    val summary: OfdSummary? = null,
    val nextRequest: Int? = null,
    val admin: Boolean = false,
    val busy: Boolean = false
) {
    /** Можно сверять: касса не занята и её очередь отправки пуста. */
    val syncable: Boolean get() = kkm != null && !busy && KkmSettingRules.syncable(kkm)

    /** Сведения о кассе касса сверяет только при закрытой смене. */
    val serviceSyncable: Boolean get() = kkm != null && !busy && KkmSettingRules.serviceSyncable(kkm)

    /** Новый токен касса принимает в режиме программирования. */
    val tokenEditable: Boolean get() = kkm?.let { KkmSettingRules.met(KkmSettingRules.branding(it)) } == true && !busy
}
