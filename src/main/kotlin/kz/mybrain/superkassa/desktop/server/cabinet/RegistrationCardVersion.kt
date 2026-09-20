@file:OptIn(ExperimentalSerializationApi::class)

package kz.mybrain.superkassa.desktop.server.cabinet

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

/**
 * Версия регистрационной карты кассы.
 *
 * Карта отражает состояние кассы на сегодня, и при перерегистрации
 * прежняя переписывается: адрес, РКА и торговая точка становятся новыми,
 * а того, что было записано в КГД до заявления, в действующей карте
 * не остаётся. Между тем нужна именно прежняя — ею подтверждают, где
 * касса стояла в те дни, за которые спрашивают. Кабинет их хранит
 * и отдаёт списком; приложение показывало только действующую.
 *
 * Разбор снисходительный, как и у остальной стороны кабинета: имена
 * полей ещё сдвинутся, поэтому у спорных перечислены возможные,
 * а недостающее показывается прочерком.
 */
@Serializable
data class RegistrationCardVersion(
    @JsonNames("versionNumber", "number") val version: Int = 0,
    val status: String? = null,
    @JsonNames("openedAt", "createdAt") val validFrom: String? = null,
    @JsonNames("closedAt") val validTo: String? = null,
    /** Каким регистрационным действием версия открыта: постановка на учёт или перерегистрация. */
    @JsonNames("openedByActionType", "openAction") val openedBy: String? = null,
    /** Чем закрыта: перерегистрацией или снятием с учёта; у действующей — ничего. */
    @JsonNames("closedByActionType", "closeAction") val closedBy: String? = null,
    /** Что в этой версии стало другим против предыдущей. */
    @JsonNames("changedFields", "changes") val changed: List<String> = emptyList(),
    @JsonNames("active", "isCurrent") val current: Boolean = false
) {
    /** Версия, которая действует сейчас: она же и открыта последней. */
    val open: Boolean get() = current || validTo.isNullOrBlank()
}
