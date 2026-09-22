package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.desktop.server.cabinet.KkmMapView

/**
 * Парк касс для проверок учёта.
 *
 * Состав один на счёт и на снимки: набранный в каждом наборе заново,
 * он разошёлся бы мелочами — где-то у кассы оказался бы номер КГД,
 * где-то нет, — и разница на картинке была бы не та, которую проверяют.
 *
 * Главный состав — в пропорции показа: из 3294 касс на учёте четыре,
 * две сняты, остальные заведены и ждут КГД. Так выглядит сеть, которую
 * увидит гость, и проверять счёт нужно именно на ней.
 */
internal object RecordFleet {

    /** Столько касс в сети показа и столько из них уже учтено. */
    const val WHOLE = 3294
    const val ON_RECORD = 4
    const val DEREGISTERED = 2

    /** Области, по которым разложен парк: адрес начинается с области. */
    val REGIONS = listOf("Алматы", "Астана", "Шымкент", "Актобе", "Караганда")

    /**
     * Сеть показа: черновики, четыре учтённые кассы и две снятые.
     *
     * Кассы разложены по областям по остатку от деления: разложение
     * не зависит от длины набора, и счёт по областям проверяется
     * и на пяти кассах, и на трёх тысячах.
     */
    fun show(count: Int = WHOLE): List<AnalyticsKkm> = (1..count).map { at ->
        val status = when {
            at <= ON_RECORD -> "REGISTERED"
            at <= ON_RECORD + DEREGISTERED -> "DEREGISTERED"
            else -> "DRAFT"
        }
        kkm(at, status)
    }

    /** Смешанный парк: к учтённым и черновикам добавлены отказы и блокировки. */
    fun mixed(): List<AnalyticsKkm> = listOf(
        kkm(1, "REGISTERED", shift = "OPEN"),
        kkm(2, "REGISTERED", shift = "CLOSE", blocked = true),
        kkm(3, "REGISTERED_REREGISTRATION_SUCCESS", shift = "OPEN"),
        kkm(4, "REGISTRATION_REFUSED"),
        kkm(5, "REREGISTRATION_REFUSED", blocked = true),
        kkm(6, "DRAFT"),
        kkm(7, "REGISTRATION_IN_ISNA_PROCESS"),
        kkm(8, "DEREGISTERED"),
        // Касса без адреса и без точки: в парк она входит, а области
        // у неё нет — свод обязан сказать об этом словами.
        kkm(9, "DRAFT", address = null, place = null),
        // Заведённая касса с открытой сменой: торгующей она не считается,
        // потому что торговать по закону ей ещё нечем.
        kkm(10, "DRAFT", shift = "OPEN")
    )

    /**
     * Парк, в котором отказов много.
     *
     * Полсотни отказов — это список длиннее окна, и на нём проверяется
     * то, ради чего вкладка собрана ленивым списком: она прокручивается,
     * а не обрывается на высоте окна.
     */
    fun refusals(count: Int): List<AnalyticsKkm> =
        (1..count).map { at -> kkm(at, "REGISTRATION_REFUSED") }

    /** Ответ кабинета с этим парком: часть касс он ставит на карту, часть — нет. */
    fun view(kkms: List<AnalyticsKkm>): KkmMapView {
        val placed = kkms.filter { it.address != null }
        val without = kkms - placed.toSet()
        return KkmMapView(
            placedCount = placed.size,
            withoutPositionCount = without.size,
            placed = placed,
            withoutPosition = without
        )
    }

    fun kkm(
        at: Int,
        status: String,
        shift: String? = "CLOSE",
        blocked: Boolean = false,
        place: String? = "Магазин $at",
        address: String? = "${REGIONS[at % REGIONS.size]}, проспект Абая, $at"
    ) = AnalyticsKkm(
        cashRegisterId = "c$at",
        kkmId = 2_000_000 + at,
        registrationNumber = "%012d".format(at),
        internalName = "Касса $at",
        retailPlaceId = place?.let { "p${at % PLACES}" },
        retailPlaceName = place,
        address = address,
        status = status,
        blocked = blocked,
        shiftStatus = shift,
        shiftNumber = at.toLong(),
        lastContactAt = "2026-09-19T08:14:00Z"
    )

    /**
     * Сколько торговых точек в наборе.
     *
     * Кратно числу областей: тогда точка целиком лежит в своей области,
     * как и в жизни, и счёт точек по областям проверяется числом.
     */
    const val PLACES = 15
}
