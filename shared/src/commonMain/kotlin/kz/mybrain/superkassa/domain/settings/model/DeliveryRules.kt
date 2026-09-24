package kz.mybrain.superkassa.domain.settings.model

import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings
import kz.mybrain.superkassa.domain.workplace.model.ServiceAddress

/**
 * Правила настроек доставки чека: что показать, что принять, что записать.
 *
 * Ключи каналов на экран не выходят: экран настроек показывают и снимают,
 * и ключ бота или пароль почты уехали бы в чужой снимок. Заданный ключ
 * показывается знаком [HIDDEN]; знак, оставленный как есть, значит «ключ
 * прежний», стёртый — «ключа нет», набранное — новый ключ.
 */
object DeliveryRules {

    /** Знак заданного ключа на экране. */
    const val HIDDEN: String = "***"

    /** Наибольший номер порта почтового сервера. */
    private const val MAX_PORT = 65_535

    /** Что стоит в поле: значение, у ключа — знак, у пустого — пустая строка. */
    fun shown(settings: DeliverySettings?, field: DeliveryField): String {
        val value = settings?.let(field.read)?.takeIf { it.isNotBlank() } ?: return ""
        return if (field.secret) HIDDEN else value
    }

    /**
     * Набранное в поле ключа.
     *
     * Поле показывает знак заданного ключа; первое нажатие по нему — начало
     * нового ключа, а не приписка к знаку, и первое стирание стирает ключ
     * целиком, а не одну звёздочку.
     */
    fun typedSecret(shown: String, typed: String): String = when {
        shown != HIDDEN -> typed
        typed.startsWith(HIDDEN) -> typed.removePrefix(HIDDEN)
        HIDDEN.startsWith(typed) -> ""
        else -> typed
    }

    /** Годится ли набранное в поле: пустое годится всегда — это «не задано». */
    fun valid(field: DeliveryField, text: String): Boolean {
        val value = text.trim()
        if (value.isEmpty() || field.secret && value == HIDDEN) return true
        return when (field) {
            DeliveryField.SmsUrl -> ServiceAddress.valid(value)
            DeliveryField.EmailPort -> value.toIntOrNull()?.let { it in 1..MAX_PORT } == true
            DeliveryField.EmailFrom -> '@' in value && value.none(Char::isWhitespace)
            else -> value.none(Char::isWhitespace)
        }
    }

    /**
     * Настройки доставки с набранным.
     *
     * Поля, которых не касались, остаются как были; ключ под знаком
     * [HIDDEN] — прежним.
     */
    fun applied(
        settings: DeliverySettings?,
        typed: Map<DeliveryField, String>,
        switched: Map<DeliveryChannel, Boolean> = emptyMap()
    ): DeliverySettings {
        val fields = DeliveryField.entries.fold(settings ?: DeliverySettings()) { now, field ->
            val text = typed[field]?.trim() ?: return@fold now
            if (field.secret && text == HIDDEN) now else field.write(now, text.ifEmpty { null })
        }
        return switched.entries.fold(fields) { now, (channel, on) -> now.withRoute(channel) { copy(enabled = on) } }
    }

    /** Каналы, по которым касса шлёт каждый чек. */
    fun enabled(settings: DeliverySettings?): Set<DeliveryChannel> =
        DeliveryChannel.entries.filter { settings?.route(it)?.enabled == true }.toSet()

    /**
     * Каналы, которые касса сочтёт настроенными.
     *
     * Правило то же, что у кассы: у канала без адреса или ключа покупатель
     * получит отказ «канал не настроен».
     */
    fun configured(settings: DeliverySettings?): Set<DeliveryChannel> = DeliveryChannel.entries.filter { channel ->
        val fields = DeliveryField.of(channel).filter { it.required }
        settings != null && fields.all { !settings.let(it.read).isNullOrBlank() }
    }.toSet()

    /** Без этого поля касса канал настроенным не сочтёт. */
    private val DeliveryField.required: Boolean
        get() = this in REQUIRED

    private val REQUIRED = setOf(
        DeliveryField.SmsUrl,
        DeliveryField.TelegramToken,
        DeliveryField.WhatsAppToken,
        DeliveryField.WhatsAppSender,
        DeliveryField.EmailHost
    )
}
