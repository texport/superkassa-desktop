package kz.mybrain.superkassa.integrations.bfdcabinet

/**
 * Неудача обращения к кабинету.
 *
 * Четыре случая, и путать их нельзя: кабинет отказал по существу, ответил
 * непонятно, доступ кончился или кабинет не ответил вовсе. Слов для владельца
 * здесь нет — их подбирает приложение на его языке по виду неудачи и коду.
 */
sealed class CabinetFailure(message: String, cause: Throwable?) : Exception(message, cause)

/**
 * Отказ кабинета по существу — его кодом и его словами.
 *
 * @property code код отказа кабинета; нет кода — `HTTP_<состояние>`.
 * @property text что кабинет сказал: сказанное о полях, иначе `detail`,
 *   иначе `title` — на языке кабинета.
 * @property httpStatus состояние ответа: `404` у аналитики значит «раздел
 *   ещё не выложен», а не «не найдено».
 */
class CabinetRefusal(val code: String, val text: String, val httpStatus: Int) :
    CabinetFailure("$code: $text", null)

/**
 * Кабинет ответил успехом, а прочитать ответ нельзя: разошёлся договор.
 *
 * От недоступности это отличается всем — кабинет отвечает и работает.
 *
 * @property path ручка, чей ответ не прочитан.
 */
class CabinetUnreadable(val path: String, cause: Throwable) : CabinetFailure("$path: ${cause.message}", cause)

/**
 * Доступ кончился: срок истёк, кабинет его больше не принимает или владелец
 * не входил. Не отказ, а конец сеанса: владельца возвращают ко входу.
 */
class CabinetExpired(cause: Throwable? = null) : CabinetFailure("cabinet access expired", cause)

/**
 * Кабинет не ответил: нет соединения или истекло ожидание.
 *
 * Запрос мог дойти: истёкшее ожидание ответа не повторяется, иначе второе
 * заявление было бы вторым заявлением.
 */
class CabinetUnreachable(cause: Throwable) : CabinetFailure("cabinet unreachable: ${cause::class.simpleName}", cause)
