package kz.mybrain.superkassa.strings.api.settings

/**
 * Надписи сведений о кассе на этой машине и данных её авторизации в БФД.
 *
 * Своим файлом: сведения читают до входа, когда поддержка разбирает кассу,
 * которая не открылась или не пускает, и слов у них своя дюжина.
 */
data class KassaFactsTexts(
    val title: String,
    val hint: String,
    val appVersion: String,
    val coreVersion: String,
    val dataDirectory: String,
    val kkmCount: String,
    val unread: String,
    val ofdAuth: String,
    val nextRequest: String
)
