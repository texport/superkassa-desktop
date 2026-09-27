package kz.mybrain.superkassa.integrations.egovmobile

import kotlin.time.Instant

/**
 * Процедура подписи у посредника: заведена, данные ждут передачи.
 *
 * Данные передаются посреднику уже при открытом окне подписи: посредник
 * держит этот запрос, пока eGov mobile их не заберёт, — и ссылка запуска
 * с QR нужны владельцу раньше, чем запрос ответит.
 *
 * @property launch ссылка, открывающая eGov mobile с этой процедурой на том же устройстве.
 * @property qr QR для eGov mobile на другом устройстве — картинка PNG.
 * @property expiresAt до какого мгновения посредник держит процедуру; `null` — не сказал.
 */
class EgovProcedure internal constructor(
    val launch: String,
    val qr: ByteArray,
    val expiresAt: Instant?,
    internal val signUrl: String,
    internal val dataUrl: String,
    internal val payload: String
)
