package kz.mybrain.superkassa.presentation.strings.kassa

import kz.mybrain.superkassa.domain.kassa.model.ContactKind
import kz.mybrain.superkassa.presentation.strings.common.Language

/**
 * Надписи контакта покупателя — одни для продажи и возврата.
 *
 * @property kinds название вида контакта на сегменте.
 * @property labels подпись поля для каждого вида.
 * @property formats как набрать контакт этого вида: подсказка под полем, когда набранное не разобрано.
 * @property hint подсказка под пустым полем: зачем оно.
 * @property sendsTo подсказка под разобранным полем: куда уйдёт чек.
 */
data class BuyerContactTexts(
    val kinds: Map<ContactKind, String>,
    val labels: Map<ContactKind, String>,
    val formats: Map<ContactKind, String>,
    val hint: String,
    val sendsTo: String
)

/** Надписи контакта покупателя на выбранном языке. */
fun buyerContactTexts(language: Language): BuyerContactTexts = when (language) {
    Language.Kk -> kazakhContact
    Language.Ru -> russianContact
    Language.En -> englishContact
}

private val russianContact = BuyerContactTexts(
    kinds = mapOf(ContactKind.Phone to "Телефон", ContactKind.Email to "Почта", ContactKind.Telegram to "Telegram"),
    labels = mapOf(
        ContactKind.Phone to "Телефон покупателя",
        ContactKind.Email to "Почта покупателя",
        ContactKind.Telegram to "Чат покупателя в Telegram (ID)"
    ),
    formats = mapOf(
        ContactKind.Phone to "Номер Казахстана: +7 7XX XXX XX XX",
        ContactKind.Email to "Почта вида name@example.kz",
        ContactKind.Telegram to "Номер чата цифрами: бот пишет только по нему"
    ),
    hint = "Необязательно: чек уйдёт покупателю на этот контакт",
    sendsTo = "Чек уйдёт на"
)

private val kazakhContact = BuyerContactTexts(
    kinds = mapOf(ContactKind.Phone to "Телефон", ContactKind.Email to "Пошта", ContactKind.Telegram to "Telegram"),
    labels = mapOf(
        ContactKind.Phone to "Сатып алушының телефоны",
        ContactKind.Email to "Сатып алушының поштасы",
        ContactKind.Telegram to "Сатып алушының Telegram чаты (ID)"
    ),
    formats = mapOf(
        ContactKind.Phone to "Қазақстан нөмірі: +7 7XX XXX XX XX",
        ContactKind.Email to "name@example.kz түріндегі пошта",
        ContactKind.Telegram to "Чат нөмірі цифрмен: бот тек соған жазады"
    ),
    hint = "Міндетті емес: чек сатып алушыға осы байланысқа жіберіледі",
    sendsTo = "Чек жіберіледі:"
)

private val englishContact = BuyerContactTexts(
    kinds = mapOf(ContactKind.Phone to "Phone", ContactKind.Email to "Email", ContactKind.Telegram to "Telegram"),
    labels = mapOf(
        ContactKind.Phone to "Customer phone",
        ContactKind.Email to "Customer email",
        ContactKind.Telegram to "Customer Telegram chat (ID)"
    ),
    formats = mapOf(
        ContactKind.Phone to "A Kazakhstan number: +7 7XX XXX XX XX",
        ContactKind.Email to "An address like name@example.kz",
        ContactKind.Telegram to "The chat number in digits: the bot writes only to it"
    ),
    hint = "Optional: the receipt goes to the customer at this contact",
    sendsTo = "The receipt goes to"
)
