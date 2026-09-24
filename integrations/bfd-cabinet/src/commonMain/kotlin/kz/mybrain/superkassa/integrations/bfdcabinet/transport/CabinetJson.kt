package kz.mybrain.superkassa.integrations.bfdcabinet.transport

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kz.mybrain.superkassa.integrations.bfdcabinet.CabinetUnreadable

/**
 * Разбор кабинета — снисходительный.
 *
 * Сторону кабинета дописывают сейчас: незнакомые поля пропускаются,
 * `null` в запросах не пишется, числа в кавычках читаются числами.
 */
internal val cabinetJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

/** Ответ кабинета в тип [T]; неразборчивый — [CabinetUnreadable], а не падение. */
internal inline fun <reified T> decoded(path: String, text: String): T =
    decoded(path) { cabinetJson.decodeFromString<T>(text) }

/**
 * Разбор ответа ручки [path].
 *
 * Разошедшийся договор — поле сменило имя или тип — даёт [CabinetUnreadable]:
 * от недоступности службы он отличается всем, и сводить их к одному нельзя.
 */
internal inline fun <T> decoded(path: String, read: () -> T): T = try {
    read()
} catch (mismatch: IllegalArgumentException) {
    throw CabinetUnreadable(path, mismatch)
}

/** Тело запроса JSON-текстом. */
internal inline fun <reified T> encoded(value: T): String = cabinetJson.encodeToString(value)

/**
 * Отказ кабинета, как он приходит по сети.
 *
 * Кабинет отвечает по RFC 9457: причина в `detail`, заголовок в `title`;
 * прежняя форма с `message` тоже читается.
 */
@Serializable
internal class CabinetError(
    val code: String? = null,
    val message: String? = null,
    val detail: String? = null,
    val title: String? = null,
    val errors: List<CabinetFieldError> = emptyList()
) {
    /**
     * Что кабинет сказал.
     *
     * Сказанное о полях идёт первым: на неверный ввод кабинет отвечает общим
     * `detail` — «Validation failure», — а по существу говорит в `errors`,
     * про то поле, которое надо исправить.
     */
    fun text(): String? {
        val fields = errors.mapNotNull { it.message }.joinToString("; ").takeIf { it.isNotBlank() }
        return listOfNotNull(fields, message, detail, title).firstOrNull { it.isNotBlank() }
    }
}

/** Отказ по одному полю. */
@Serializable
internal class CabinetFieldError(val field: String? = null, val message: String? = null)
