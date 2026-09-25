package kz.mybrain.superkassa.strings.api.cabinet.places

import kz.mybrain.superkassa.strings.api.analytics.SieveTexts

/** Колонка торговых точек и карточка точки: поиск, порядок, отбор и правка. */
data class PlaceTexts(
    val title: String,
    val empty: String,
    val add: String,
    val name: String,
    val address: String,
    val place: String,
    val registerCount: String,
    val registers: String,
    val registersEmpty: String,
    val chooseRegister: String,
    val pickRegisterFirst: String,
    val rename: String,
    val changeAddress: String,
    val addressChanged: String,
    val addressNeedsReregistration: String,
    val exists: String,
    val removeBlocked: String,
    val latitude: String,
    val longitude: String,
    val pickOnMap: String,
    val pointNotChosen: String,

    /** Поиск по колонке торговых точек: их бывают сотни. */
    val search: String,
    val notFound: String,
    val shownOf: String,

    /**
     * Слова отбора касс — те же, что на карте аналитики: [SieveTexts].
     *
     * Состояние учёта владелец спрашивает и там, и в колонке точек;
     * своего набора слов колонка не заводит.
     */
    val sieve: SieveTexts,

    /**
     * По чему колонка упорядочена.
     *
     * Названо «по чему», а не «чем»: владелец выбирает, по какому
     * признаку выстроить две тысячи точек, а не как их считать.
     */
    val orderByName: String,
    val orderByAddress: String,
    val orderByRegisters: String,

    /**
     * Порядок по состоянию учёта.
     *
     * От того, во что надо вмешаться, к тому, во что не надо: сначала
     * отказы КГД, потом ожидание ответа, потом заведённые, и только
     * в конце работающие и снятые с учёта. Две тысячи точек, у которых
     * всё в порядке, ни о чём владельца не спрашивают.
     */
    val orderByRecord: String,
    val ascending: String,
    val descending: String
)
