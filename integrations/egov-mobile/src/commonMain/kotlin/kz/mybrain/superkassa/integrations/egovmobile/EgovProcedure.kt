package kz.mybrain.superkassa.integrations.egovmobile

import kotlin.time.Instant

/**
 * Процедура подписи у посредника: данные переданы, eGov mobile может их забрать.
 *
 * @property launch ссылка, открывающая eGov mobile с этой процедурой на том же устройстве.
 * @property qr QR для eGov mobile на другом устройстве — картинка PNG.
 * @property expiresAt до какого мгновения посредник держит процедуру; `null` — не сказал.
 */
class EgovProcedure internal constructor(
    val launch: String,
    val qr: ByteArray,
    val expiresAt: Instant?,
    internal val signUrl: String
)
