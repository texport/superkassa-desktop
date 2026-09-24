package kz.mybrain.superkassa.domain.settings.model

/**
 * Что касса на этой машине рассказывает о себе поддержке.
 *
 * Первое, что спрашивают при разборе: какая версия приложения и ядра,
 * где лежат данные и сколько касс заведено. Секретов здесь нет — ни
 * токена, ни пинов, ни адреса и учётных данных сервера базы, — поэтому
 * сведения видны и до входа: поддержке они нужны как раз тогда, когда
 * касса не открылась или не пускает.
 *
 * @property appVersion версия приложения.
 * @property coreVersion версия ядра, с которым собрано приложение.
 * @property directory каталог данных кассы; `null` — платформа его не назвала.
 * @property kkmCount касс, заведённых на этой машине; `null` — касса не ответила.
 */
data class KassaFacts(
    val appVersion: String,
    val coreVersion: String,
    val directory: String?,
    val kkmCount: Int?
)
