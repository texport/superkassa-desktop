package kz.mybrain.superkassa.integrations.bfdcabinet

import kotlinx.serialization.Serializable

/**
 * Страница списка кабинета.
 *
 * @property page номер страницы с нуля.
 * @property size сколько записей на странице.
 * @property totalElements сколько записей у кабинета всего.
 * @property items записи страницы.
 */
@Serializable
data class CabinetPage<T>(
    val page: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0,
    val items: List<T> = emptyList()
)

/**
 * Записей на странице — одинаково у всех списков кабинета; больше он не отдаёт.
 *
 * Константа файла, а не `companion` класса страницы: свой companion
 * у `@Serializable`-класса легко спутать с тем, где сериализация ищет разборщик.
 */
const val CABINET_PAGE_SIZE: Int = 50
