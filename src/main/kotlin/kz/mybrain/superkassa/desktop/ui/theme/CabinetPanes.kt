package kz.mybrain.superkassa.desktop.ui.theme

/**
 * Раскладки раздела торговых точек кабинета.
 *
 * Наименьшие ширины — те же, что у общей пары [Panes.placesAndCard]:
 * колонка и карточка встают рядом и сменяют друг друга на тех же порогах.
 * Отличается раздел доли.
 *
 * Карточка точки и кассы — форма, и шире строки чтения
 * ([ContentWidths.reading]) она не растёт: лишнее место ей ни к чему.
 * Колонке же каждая точка ширины — это строка списка по высоте: в узкой
 * колонке поиск, отбор и кнопки переносятся на вторую и третью строку,
 * и в окне 960×640 из двух тысяч точек оставалась видна одна. Поэтому
 * до предела карточки место делится поровну, а сверх него уходит колонке.
 */
object CabinetPanes {

    /** Колонка точек и касс и карточка выбранного. */
    val placesAndReadingCard = PaneSplit(
        firstShare = HALF,
        firstMin = Panes.placesAndCard.firstMin,
        secondMin = Panes.placesAndCard.secondMin,
        secondMax = ContentWidths.reading
    )

    /**
     * Свёрнутая колонка рельсом и карточка рядом.
     *
     * Свёрнутой колонке не нужно ничего сверх ширины рельса, и всё
     * остальное — карточке, как и было до раскладки долями.
     */
    val railAndCard = PaneSplit(
        firstShare = 0f,
        firstMin = Sizes.rail,
        secondMin = Panes.placesAndCard.secondMin
    )

    private const val HALF = 0.5f
}
