package kz.mybrain.superkassa.domain.kassa.model

import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings

/**
 * Какими видами контакта касса может отправить чек покупателю.
 *
 * Доставка чека необязательна: «не отправлять» доступно всегда, а вид
 * контакта — только когда настроен хотя бы один его канал доставки ядра
 * ([ContactKind.channels]). Ненастроенный вид выбрать нельзя: чек ушёл бы
 * в канал, который ответит покупателю отказом «не настроен», а кассир
 * пообещал бы чек, которого не будет.
 *
 * @property ready виды контакта, чей канал настроен; пусто — доставки нет вовсе,
 *   и чек можно только показать покупателю или распечатать.
 */
data class ContactChannels(val ready: Set<ContactKind> = emptySet()) {

    /** Ни один канал не настроен: выбирать нечего. */
    val none: Boolean get() = ready.isEmpty()

    /** Можно ли выбрать этот вид: «не отправлять» — всегда, прочие — по каналу. */
    fun allows(kind: ContactKind): Boolean = kind == ContactKind.None || kind in ready

    /** Контакт другого вида; вид с ненастроенным каналом не выбирается. */
    fun choose(contact: BuyerContact, kind: ContactKind): BuyerContact =
        if (allows(kind)) contact.switchTo(kind) else contact

    /**
     * Контакт, годный при этих каналах.
     *
     * Канал выбранного вида перестал быть настроен — пока кассир был
     * в настройках — и контакт становится «не отправлять»: иначе чек ушёл
     * бы в канал, которого нет.
     */
    fun fit(contact: BuyerContact): BuyerContact = if (allows(contact.kind)) contact else BuyerContact()

    /** Каналы по настройкам доставки ядра. */
    companion object {

        /**
         * Настроенные виды контакта.
         *
         * Канал настроен, когда он включён в маршрутах доставки и у него
         * есть то, без чего ядро его не поднимет: адрес шлюза SMS, ключ
         * бота Telegram, ключ и номер отправителя WhatsApp, почтовый сервер.
         * Правило то же, что у каналов ядра: без этого канал отвечает
         * отказом «не настроен».
         *
         * @param settings настройки доставки; `null` — не настроено ничего.
         */
        fun of(settings: DeliverySettings?): ContactChannels {
            val delivery = settings ?: return ContactChannels()
            val kinds = ContactKind.entries.filter { kind -> kind.channels.any { delivery.ready(it) } }
            return ContactChannels(kinds.toSet())
        }

        private fun DeliverySettings.ready(channel: String): Boolean =
            channels.any { it.enabled && it.channel.equals(channel, ignoreCase = true) } && provided(channel)

        private fun DeliverySettings.provided(channel: String): Boolean = when (channel) {
            SMS -> filled(sms?.providerUrl)
            WHATSAPP -> filled(whatsapp?.accessToken) && filled(whatsapp?.phoneNumberId)
            TELEGRAM -> filled(telegram?.botToken)
            EMAIL -> filled(email?.host)
            else -> false
        }

        private fun filled(value: String?): Boolean = !value.isNullOrBlank()

        private const val SMS = "SMS"
        private const val WHATSAPP = "WHATSAPP"
        private const val TELEGRAM = "TELEGRAM"
        private const val EMAIL = "EMAIL"
    }
}
