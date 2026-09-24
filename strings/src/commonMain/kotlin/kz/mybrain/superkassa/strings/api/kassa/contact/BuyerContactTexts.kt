package kz.mybrain.superkassa.strings.api.kassa.contact

/**
 * Надписи контакта покупателя — одни для продажи и возврата.
 *
 * @property kinds название вида контакта на сегменте.
 * @property labels подпись поля для каждого вида.
 * @property formats как набрать контакт этого вида: подсказка под полем, когда набранное не разобрано.
 * @property hint подсказка под пустым полем: зачем оно.
 * @property sendsTo подсказка под разобранным полем: куда уйдёт чек.
 * @property notConfigured пометка видов, чей канал доставки не настроен.
 * @property unavailable строка вместо выбора, когда не настроен ни один канал.
 */
data class BuyerContactTexts(
    val kinds: ContactKindNames,
    val labels: ContactFieldTexts,
    val formats: ContactFieldTexts,
    val hint: String,
    val sendsTo: String,
    val notConfigured: String,
    val unavailable: String
)

/** Название каждого вида контакта: так он назван на сегменте выбора. */
data class ContactKindNames(
    /** Чек покупателю не отправляется. */
    val none: String,
    val phone: String,
    val email: String,
    val telegram: String
)

/**
 * Текст поля каждого вида контакта: подпись поля или образец набора.
 *
 * Вида «не отправлять» здесь нет: поля у него нет.
 */
data class ContactFieldTexts(
    val phone: String,
    val email: String,
    val telegram: String
)
