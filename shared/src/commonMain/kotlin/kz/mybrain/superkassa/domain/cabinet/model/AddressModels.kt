package kz.mybrain.superkassa.domain.cabinet.model

/** Адрес из регистра. */
data class RegisterAddress(
    val addressRef: String,
    val address: String? = null,
    val addressKz: String? = null,
    val rka: String? = null,
    val cato: String? = null
)

/** Подсказка адресного регистра на одном шаге каскада; `rka` заполнен только у строения. */
data class AddressSuggestion(val id: Long, val name: String, val rka: String? = null, val level: String? = null)

/**
 * Шаг адресного регистра: адрес собирается из региона, населённых пунктов,
 * улицы и дома. Пунктов может быть несколько подряд — у Караганды под
 * городом лежат районы.
 */
enum class AddressLevel { Region, Locality, Street, Building }

/**
 * Подсказки регистра, различимые в списке.
 *
 * Регистр отдаёт на один номер дома две записи — «10» и «10», — и они
 * не двойники: у них разные идентификаторы и разные коды РКА. Полный
 * двойник — то же название и тот же код РКА — убирается: выбрать из двух
 * одинаковых строк владельцу нечего.
 */
fun distinctSuggestions(items: List<AddressSuggestion>): List<AddressSuggestion> =
    items.distinctBy { it.name to it.rka }
