package kz.mybrain.superkassa.strings.api.settings

/**
 * Надписи того, что касса делает сама: её настройки на этой машине
 * и закрытие смены без кассира. Доставка чека — в [DeliverySettingTexts].
 *
 * Заведено своим файлом: настройки кассы в процессе появились вместе
 * с ней, и строк у них полтора десятка.
 */
data class CoreSettingTexts(
    val title: String,
    val hint: String,
    val unread: String,
    val mode: String,
    val modeDesktop: String,
    val modeServer: String,
    val reconnect: String,
    val seconds: String,
    val protocolFixed: String,
    val frozen: String,
    val frozenHint: String,
    val serverHint: String,
    val saved: String,
    val autoClose: String,
    val autoCloseHint: String
)
