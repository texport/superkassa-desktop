package kz.mybrain.superkassa.presentation.settings.core

import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import kz.mybrain.superkassa.domain.settings.model.DeliveryChannel
import kz.mybrain.superkassa.domain.settings.model.DeliveryField
import kz.mybrain.superkassa.domain.settings.model.DeliveryRules
import kz.mybrain.superkassa.domain.settings.model.frozen
import kz.mybrain.superkassa.domain.settings.model.server

/**
 * Каналы доставки чека, как их видит владелец.
 *
 * Самих ключей здесь нет: из настроек кассы берётся только то, что стоит
 * в полях, а ключ стоит там знаком — см. [DeliveryRules.shown].
 *
 * @property read настройки прочитаны: до этого полей нет.
 * @property saved что стоит в полях по записи кассы.
 * @property drafts набранное по полям; поле, которого не касались, не в нём.
 * @property switched включённые и выключенные владельцем каналы; нетронутые не в нём.
 * @property enabledSaved каналы, включённые по записи кассы.
 * @property configured каналы, которые касса сочтёт настроенными.
 * @property frozen правка закрыта владельцем или режимом сервера.
 * @property server касса работает сервером.
 */
data class DeliveryUiState(
    val read: Boolean = false,
    val saved: Map<DeliveryField, String> = emptyMap(),
    val drafts: Map<DeliveryField, String> = emptyMap(),
    val switched: Map<DeliveryChannel, Boolean> = emptyMap(),
    val enabledSaved: Set<DeliveryChannel> = emptySet(),
    val configured: Set<DeliveryChannel> = emptySet(),
    val frozen: Boolean = false,
    val server: Boolean = false,
    val busy: Boolean = false
) {
    /** Что стоит в поле: набранное, иначе записанное. */
    fun value(field: DeliveryField): String = drafts[field] ?: saved[field].orEmpty()

    /** Шлёт ли канал каждый чек: переключённое, иначе записанное. */
    fun enabled(channel: DeliveryChannel): Boolean = switched[channel] ?: (channel in enabledSaved)

    /** Набранное в поле негодно: названо под полем до сохранения. */
    fun malformed(field: DeliveryField): Boolean = !DeliveryRules.valid(field, value(field))

    /** В поле ключа стоит знак заданного ключа, а не сам ключ. */
    fun hidden(field: DeliveryField): Boolean = field.secret && value(field) == DeliveryRules.HIDDEN

    /** Набранное отличается от записанного, всё годно, и касса примет правку. */
    val savable: Boolean
        get() = read && !frozen && !busy && changed && DeliveryField.entries.none(::malformed)

    private val changed: Boolean
        get() = drafts.any { (field, text) -> text.trim() != saved[field].orEmpty() } ||
            switched.any { (channel, on) -> on != (channel in enabledSaved) }

    companion object {
        /** Каналы из прочитанных настроек кассы; набранное начинается заново. */
        fun of(settings: CoreSettings, busy: Boolean = false) = DeliveryUiState(
            read = true,
            saved = DeliveryField.entries.associateWith { DeliveryRules.shown(settings.delivery, it) },
            enabledSaved = DeliveryRules.enabled(settings.delivery),
            configured = DeliveryRules.configured(settings.delivery),
            frozen = settings.frozen,
            server = settings.server,
            busy = busy
        )
    }
}

/** Что владелец делает с каналами доставки. Пустые действия — для снимков вида. */
interface DeliveryActions {
    fun type(field: DeliveryField, text: String) = Unit

    fun switch(channel: DeliveryChannel, on: Boolean) = Unit

    fun save() = Unit
}
