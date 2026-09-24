package kz.mybrain.superkassa.integrations.bfdcabinet.address

import kotlinx.serialization.Serializable

/**
 * Подсказка адресного регистра на одном шаге.
 *
 * Регистр отдаёт на один номер дома две записи с разными идентификаторами
 * и кодами РКА — они не двойники, и модуль их не сливает.
 *
 * @property id идентификатор записи — родитель следующего шага.
 * @property rka код РКА; заполнен только у строения.
 * @property level шаг регистра, как его называет кабинет.
 */
@Serializable
data class AddressSuggestion(val id: Long, val name: String, val rka: String? = null, val level: String? = null)

/** Подсказки шага. */
@Serializable
internal class AddressSuggestions(val items: List<AddressSuggestion> = emptyList())

/**
 * Адрес, подтверждённый регистром по коду РКА.
 *
 * @property addressRef то, что уходит в торговую точку ссылкой на адрес, — код РКА.
 * @property address адрес на русском.
 * @property addressKz адрес на казахском.
 * @property cato код КАТО населённого пункта.
 */
data class RegisterAddress(
    val addressRef: String,
    val address: String?,
    val addressKz: String?,
    val rka: String,
    val cato: String?
)

/** Ответ регистра по коду РКА. */
@Serializable
internal class ResolvedAddress(
    val rka: String,
    val cato: String? = null,
    val displayAddress: String? = null,
    val displayAddressKk: String? = null
) {
    fun address() = RegisterAddress(rka, displayAddress, displayAddressKk, rka, cato)
}
