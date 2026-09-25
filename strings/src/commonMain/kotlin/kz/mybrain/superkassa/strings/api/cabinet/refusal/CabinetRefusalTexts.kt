package kz.mybrain.superkassa.strings.api.cabinet.refusal

/**
 * Отказы кабинета и подписи — то, что владелец читает всплывающей строкой окна.
 *
 * Отказ может прийти на любом экране кабинета, поэтому слова его —
 * одним набором, а не у каждого сценария свои.
 */
data class CabinetRefusalTexts(
    val unreachable: String,

    /**
     * Кабинет ответил, но ответ не разобрался.
     *
     * Отдельно от молчания: под общими словами о недоступности разошедшийся
     * договор выглядел обрывом связи, и искать его шли не там.
     */
    val unreadable: String,
    val sessionExpired: String,
    val noNcaLayer: String,
    val signDeclined: String,
    val signWindowClosed: String,
    val signCancelled: String,
    val kkmNotActive: String,
    val defaultPinNotAllowed: String
)
