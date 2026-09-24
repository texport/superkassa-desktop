package kz.mybrain.superkassa.domain.cabinet.model

/**
 * Ответы кабинета: владелец, компания и виды деятельности.
 *
 * Время приходит строкой ISO-8601 и хранится строкой: в кабинете оно
 * нужно только для показа, а разбор ради показа стоил бы своего типа
 * и своего часового пояса.
 */

/** Владелец, вошедший по ЭЦП. */
data class CabinetUser(val id: String, val iin: String, val fullName: String)

/** Компания владельца. */
data class CabinetCompany(val id: String, val bin: String, val name: String)

/** Вид деятельности компании. */
data class Oked(
    val code: String,
    val name: String? = null,
    /** Основной ли это вид деятельности: у компании он один. */
    val primary: Boolean = false
)

/** Компания и её виды деятельности. */
data class CompanyProfile(
    val id: String,
    val bin: String,
    val name: String,
    val okeds: List<Oked> = emptyList()
)

/** Позиция классификатора ОКЭД, какой её отдаёт кабинет. */
data class OkedEntry(
    val code: String,
    val name: String,
    val nameKz: String? = null,
    /** Уровень подробности: раздел, группа, класс, подкласс, вид. */
    val level: String? = null
)

/** Ответ классификатора: обёртка списка той же формы, что у адресных подсказок. */
data class OkedSuggestions(
    val items: List<OkedEntry> = emptyList(),
    /**
     * Сколько позиций подходит под запрос целиком.
     *
     * Классификатор — 2107 позиций, за раз кабинет отдаёт не больше
     * пятидесяти. Без общего числа список из пятидесяти выглядел
     * оборванным, и владелец решал, что его вида деятельности нет.
     */
    val total: Long = 0
)

/**
 * Размер страницы классификатора видов деятельности.
 *
 * Классификатор — 2107 позиций, и просить их все нельзя: кабинет
 * ограничивает страницу пятьюдесятью. Прежде здесь стояло то же число,
 * что и для шагов адресного регистра, — двадцать, — и страниц не было
 * вовсе: доскроллить до своего вида не получалось ни при каком запросе.
 * Неполная страница — последняя.
 */
const val OKED_PAGE: Int = 50

/** Кто вошёл в кабинет: владелец и его компания — всегда вместе. */
data class CabinetOwner(val user: CabinetUser, val company: CabinetCompany)
