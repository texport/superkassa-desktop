package kz.mybrain.superkassa.desktop.ui.sale

/**
 * Цена и количество, спрошенные у кассира для позиции без цены.
 *
 * Национальный каталог цен не несёт: карточка описывает товар, а цену
 * назначает продавец. Найденная в каталоге позиция вставала в чек нулевой
 * и молча, и от пробития её удерживала только закрытая смена.
 *
 * Разбор и проверки — те же, что у ручного ввода позиции: [PositionDraft]
 * знает и предел точности денег, и запрет дробного количества у штуки.
 * Второй набор правил здесь разошёлся бы с первым на первой же правке.
 *
 * @param found позиция каталога: за наименование, НТИН и единицу отвечает
 * он, за цену и количество — кассир.
 */
data class PriceAsk(
    val found: Position,
    val price: String = "",
    val quantity: String = DEFAULT_QUANTITY
) {
    private val draft: PositionDraft
        get() = PositionDraft(
            name = found.name,
            price = price,
            quantity = quantity,
            vatGroup = found.vatGroup,
            measureUnitCode = found.measureUnitCode
        )

    /** Чего не хватает, чтобы позиция встала в чек. */
    val problems: List<DraftProblem> get() = draft.problems

    /** Ошибка этого поля, если она есть. */
    fun problem(field: DraftField): DraftProblem? = problems.firstOrNull { it.field == field }

    /**
     * Позиция с заданной ценой — или `null`, пока набранное её не образует.
     *
     * Каталожное берётся у найденной позиции целиком: кассир отвечает
     * за цену и количество, а НТИН и казахское наименование обязаны
     * дойти до ОФД такими, какими их отдал каталог.
     */
    val position: Position?
        get() = draft.position?.let { found.copy(price = it.price, quantity = it.quantity) }
}

/**
 * Нужно ли спрашивать цену у кассира.
 *
 * Каталог отдаёт то отсутствующую цену, то нулевую, и для чека это одно
 * и то же: нулевая строка — не продажа.
 */
fun priceMissing(position: Position): Boolean = position.price.signum() <= 0
