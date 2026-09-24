package kz.mybrain.superkassa.domain.kassa.model

import io.github.texport.superkassa.core.presentation.api.model.receipt.CustomerContactRequest

/**
 * Каким контактом покупатель получит чек.
 *
 * Вид выбирает кассир, а не угадывает касса: номер чата в Telegram —
 * такие же десять цифр, что и телефон без кода страны, и по одной строке
 * их не различить.
 *
 * Доставка чека необязательна: [None] — чек покупателю не отправляется,
 * его показывают или печатают. С него начинается каждая продажа.
 *
 * @property channels каналы доставки ядра, которыми уходит чек на контакт
 *   этого вида: телефон — SMS или WhatsApp, почта — почта, Telegram — Telegram.
 */
enum class ContactKind(val channels: List<String>) {
    None(emptyList()),
    Phone(listOf("SMS", "WHATSAPP")),
    Email(listOf("EMAIL")),
    Telegram(listOf("TELEGRAM"))
}

/**
 * Контакт покупателя, как его набрал кассир.
 *
 * Необязателен: без контакта чек покупателю не уходит, и это обычный чек;
 * по умолчанию вид — «не отправлять».
 * Набранный — должен разбираться: касса отправила бы чек на «8 701» и
 * отчиталась бы о доставке в никуда.
 *
 * @property kind вид контакта.
 * @property text набранное, как есть.
 */
data class BuyerContact(val kind: ContactKind = ContactKind.None, val text: String = "") {

    /** Контакт не набран или отправлять не нужно: чек покупателю не отправляется. */
    val empty: Boolean get() = kind == ContactKind.None || text.isBlank()

    /** Набранное приведено к виду, в котором по нему шлют; `null` — не разобрано или пусто. */
    val normalized: String? get() = if (empty) null else normalize(kind, text.trim())

    /** Набрано, но не разобрано: чек с таким контактом пробивать нельзя. */
    val malformed: Boolean get() = !empty && normalized == null

    /** Другой вид контакта: набранное остаётся, кассир видит, годится ли оно новому виду. */
    fun switchTo(kind: ContactKind): BuyerContact = copy(kind = kind)

    fun enter(text: String): BuyerContact = copy(text = text)

    /** Контакт для кассы: только разобранный и только нужного вида. */
    fun toRequest(): CustomerContactRequest? = normalized?.let { value ->
        when (kind) {
            ContactKind.None -> null
            ContactKind.Phone -> CustomerContactRequest(phone = value)
            ContactKind.Email -> CustomerContactRequest(email = value)
            ContactKind.Telegram -> CustomerContactRequest(telegram = value)
        }
    }

    override fun toString(): String = "BuyerContact($kind, ***)"

    private companion object {
        /** Почта: имя, одна «@» и домен с точкой, без пробелов. */
        val EMAIL = Regex("""[^@\s]+@[^@\s]+\.[^@\s.]+""")

        /** Номер чата Telegram: бот пишет покупателю только по нему, не по имени. */
        val TELEGRAM = Regex("""\d{5,15}""")

        /** Знаки, которыми номер телефона разбивают при наборе. */
        val PHONE_SEPARATORS = setOf(' ', '-', '(', ')')

        /** Цифр в номере Казахстана без кода страны: 7XX XXX XX XX. */
        const val NATIONAL = 10

        /** Все номера Казахстана — и сотовые, и городские — начинаются с семёрки. */
        const val NATIONAL_FIRST = '7'
        const val COUNTRY = "+7"

        /** Код страны без плюса и выход на межгород «8»: оба стоят перед десятью цифрами номера. */
        const val COUNTRY_DIGIT = '7'
        const val TRUNK = '8'
        const val EMAIL_MAX = 254

        fun normalize(kind: ContactKind, text: String): String? = when (kind) {
            ContactKind.None -> null
            ContactKind.Phone -> phone(text)
            ContactKind.Email -> text.takeIf { it.length <= EMAIL_MAX && EMAIL.matches(it) }
            ContactKind.Telegram -> text.takeIf { TELEGRAM.matches(it) }
        }

        /**
         * Телефон Казахстана в виде +77XXXXXXXXX. Принимается как его
         * диктуют: «+7 701 765 43 21», «8 (701) 765-43-21», «7017654321».
         */
        fun phone(text: String): String? {
            val international = text.startsWith("+")
            val body = text.removePrefix("+").filterNot { it in PHONE_SEPARATORS }
            if (body.any { !it.isDigit() }) return null
            return nationalOf(body, international)?.takeIf { it[0] == NATIONAL_FIRST }?.let { COUNTRY + it }
        }

        /** Десять цифр номера: без кода страны, с «7» или «8» впереди; после «+» — только «7». */
        fun nationalOf(body: String, international: Boolean): String? = when {
            body.length == NATIONAL && !international -> body
            body.length != NATIONAL + 1 -> null
            body[0] == COUNTRY_DIGIT -> body.drop(1)
            body[0] == TRUNK && !international -> body.drop(1)
            else -> null
        }
    }
}
