package kz.mybrain.superkassa.domain.setup.port

import kz.mybrain.superkassa.domain.setup.model.CabinetRecord
import kz.mybrain.superkassa.domain.setup.model.RegistrationToSign

/**
 * Кабинет БФД глазами мастера подключения: касса в кабинете, заявление
 * о постановке на учёт и технический токен.
 *
 * Мастеру нужно от кабинета ровно это, и знать остальное — точки,
 * документы, карты — ему незачем. Обращения идут от имени владельца,
 * вошедшего в кабинет; вход — дело кабинета окна, а не мастера.
 *
 * Отказ кабинета, истёкший доступ и отказ подписи приходят исключениями
 * реализации: называет их владельцу кабинет окна — одними словами для
 * мастера и для разделов кабинета.
 */
interface SetupCabinet {

    /** Касса в кабинете: состояние учёта и номер КГД, если он уже выдан. */
    suspend fun record(registerId: String): CabinetRecord

    /** Кабинет готовит заявление о постановке на учёт и отдаёт то, что подписывается. */
    suspend fun prepareRegistration(registerId: String): RegistrationToSign

    /** Подпись ключом владельца: CMS в base64. Длится, пока владелец выбирает ключ. */
    suspend fun sign(payload: String): String

    /** Подписанное заявление уходит в ИСНА. */
    suspend fun sendRegistration(registerId: String, actionId: String, signature: String)

    /**
     * Выпускает кассе технический токен; прежний БФД отзывает.
     *
     * @return токен; `null` — кабинет его не выдал.
     */
    suspend fun issueToken(registerId: String): String?
}
