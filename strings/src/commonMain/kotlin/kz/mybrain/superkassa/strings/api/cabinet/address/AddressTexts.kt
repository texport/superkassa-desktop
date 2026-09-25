package kz.mybrain.superkassa.strings.api.cabinet.address

/** Шаги адреса точки по адресному регистру: от региона до дома. */
data class AddressTexts(
    val find: String,
    val found: String,
    val chosen: String,
    val region: String,
    val locality: String,
    val street: String,
    val building: String,
    val pickAgain: String,
    val notFound: String
)
