package kz.mybrain.superkassa.domain.setup.model

import kz.mybrain.superkassa.kassa.CoreScene
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Какой контур БФД мастер подключения предлагает по умолчанию.
 *
 * Предлагал первый из справочника и попадал на контур, которого владелец
 * не выбирал. Рабочее место шлёт чеки в один контур, и это записано
 * у касс, которые на нём уже заведены.
 */
class WorkplaceOfdTest {

    private val contours = listOf("DEV", "TEST", "PROD")

    private fun kkm(contour: String?) =
        CoreScene.kkm(id = "kkm-$contour").copy(ofdId = OfdContours.PROVIDER, ofdEnvironment = contour)

    @Test
    fun `берётся контур заведённых касс, а не первый из справочника`() {
        assertEquals("PROD", OfdContours.ofWorkplace(listOf(kkm("PROD"), kkm("PROD")), contours))
    }

    @Test
    fun `при разных контурах берётся тот, которым пользуется больше касс`() {
        val kkms = listOf(kkm("TEST"), kkm("TEST"), kkm("PROD"))
        assertEquals("TEST", OfdContours.ofWorkplace(kkms, contours))
    }

    @Test
    fun `без касс остаётся первый поднятый контур`() {
        assertEquals("DEV", OfdContours.ofWorkplace(emptyList(), contours))
    }

    /** Погашенный контур выбором не станет: подставить его значило бы запереть мастер. */
    @Test
    fun `неподнятый контур в подстановку не попадает`() {
        assertEquals("DEV", OfdContours.ofWorkplace(emptyList(), listOf("TEST", "PROD", "DEV")))
    }

    @Test
    fun `код, которого нет в справочнике, не подставляется`() {
        assertEquals("DEV", OfdContours.ofWorkplace(listOf(kkm("НЕИЗВЕСТНЫЙ")), contours))
    }

    /** Касса контуров не назвала — подставлять нечего, и заводить некуда. */
    @Test
    fun `без справочника контура нет`() {
        assertEquals("", OfdContours.ofWorkplace(listOf(kkm("DEV")), emptyList()))
    }
}
