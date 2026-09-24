@file:OptIn(ExperimentalSerializationApi::class)

package kz.mybrain.superkassa.integrations.bfdcabinet.company

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

/**
 * Вид деятельности компании.
 *
 * @property code код ОКЭД.
 * @property name наименование; в запросе не обязательно.
 * @property primary основной ли вид. Пишется в запрос всегда, даже равным
 *   умолчанию: кабинет ждёт примитив `boolean` и на отсутствующем поле
 *   отвечает отказом разбора.
 */
@Serializable
data class Oked(
    val code: String,
    val name: String? = null,
    @EncodeDefault val primary: Boolean = false
)

/** Компания и её виды деятельности. */
@Serializable
data class CompanyProfile(
    val id: String,
    val bin: String,
    val name: String,
    val okeds: List<Oked> = emptyList()
)

/**
 * Позиция классификатора ОКЭД, какой её отдаёт кабинет.
 *
 * @property nameKz наименование на казахском.
 * @property level уровень подробности: раздел, группа, класс, подкласс, вид.
 */
@Serializable
data class OkedEntry(val code: String, val name: String, val nameKz: String? = null, val level: String? = null)

/**
 * Страница классификатора.
 *
 * @property total сколько позиций подходит под запрос целиком: без него
 *   пятьдесят строк выглядели оборванным списком.
 */
@Serializable
data class OkedSuggestions(val items: List<OkedEntry> = emptyList(), val total: Long = 0)

/** Замена набора видов деятельности целиком. */
@Serializable
internal class OkedsRequest(val okeds: List<Oked>)

/**
 * Ответ на замену видов деятельности.
 *
 * Кабинет отвечает карточкой компании; прежний отвечал одними видами
 * с `companyId`. Читаются оба.
 */
@Serializable
internal class OkedsView(val okeds: List<Oked> = emptyList())
