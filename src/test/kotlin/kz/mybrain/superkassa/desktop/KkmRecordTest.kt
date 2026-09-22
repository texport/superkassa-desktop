package kz.mybrain.superkassa.desktop

import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.desktop.ui.analytics.KkmGroup
import kz.mybrain.superkassa.desktop.ui.analytics.PlacedKkm
import kz.mybrain.superkassa.desktop.ui.analytics.groupTone
import kz.mybrain.superkassa.desktop.ui.analytics.kkmGroups
import kz.mybrain.superkassa.desktop.ui.analytics.onRecordCount
import kz.mybrain.superkassa.desktop.ui.cabinet.KkmRecord
import kz.mybrain.superkassa.desktop.ui.cabinet.kkmRecord
import kz.mybrain.superkassa.desktop.ui.cabinet.statusColor
import kz.mybrain.superkassa.desktop.ui.components.StatusTone
import kz.mybrain.superkassa.desktop.ui.components.toneColor
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Одиннадцать состояний учёта — в четыре смысла, и цвет по ним.
 *
 * Приложение спрашивало у состояния одно: на учёте касса или нет, —
 * и черновик попадал в одну кучу с отказом КГД. В кабинете показа
 * из 3294 касс 3288 черновиков, четыре на учёте и две снятые: карта
 * страны от этого краснела целиком. Числа взяты запросом к кабинету
 * на боевых данных и повторены здесь, чтобы тот же счёт не вернулся.
 */
class KkmRecordTest {

    @Test
    fun `состояния учёта разведены по смыслу`() {
        assertEquals(KkmRecord.OnRecord, kkmRecord("REGISTERED"))
        assertEquals(KkmRecord.OnRecord, kkmRecord("REGISTERED_REREGISTRATION_SUCCESS"))
        listOf(
            "DRAFT",
            "REGISTRATION_IN_ISNA_PROCESS",
            "REREGISTRATION_IN_ISNA_PROCESS",
            "DEREGISTRATION_IN_ISNA_PROCESS"
        ).forEach { assertEquals(KkmRecord.InProgress, kkmRecord(it), "«$it» — это ещё не беда") }
        listOf(
            "UNKNOWN",
            "REGISTRATION_IN_ISNA_ERROR",
            "REGISTERED_REREGISTRATION_ERROR",
            "DEREGISTRATION_ERROR"
        ).forEach { assertEquals(KkmRecord.Refused, kkmRecord(it), "«$it» — повод вмешаться") }
        assertEquals(KkmRecord.Deregistered, kkmRecord("DEREGISTERED"))
    }

    /** Кабинет не назвал состояния вовсе: тревожиться не о чем, но и учёта нет. */
    @Test
    fun `отсутствие кода не считается отказом`() {
        assertEquals(KkmRecord.InProgress, kkmRecord(null))
        assertEquals(KkmRecord.InProgress, kkmRecord("  "))
    }

    /**
     * Сеть показа целиком: черновики карту не красят.
     *
     * Прежде 3290 касс из 3294 считались неблагополучными, и ярлычок
     * был красным. Теперь красит только заблокированная касса и отказ КГД,
     * а четыре кассы на учёте делают место работающим.
     */
    @Test
    fun `сеть из черновиков не красит карту отказом`() {
        val network = place(draft = 3288, registered = 4, deregistered = 2)

        assertEquals(StatusTone.Good, groupTone(network))
        assertEquals(4, onRecordCount(network))
    }

    /** Место, где на учёте нет ни одной кассы, не зелёное: оно ещё не работает. */
    @Test
    fun `место из одних черновиков не обещает работающую точку`() {
        assertEquals(StatusTone.Idle, groupTone(place(draft = 3)))
    }

    /** Отказ КГД — беда: доля от десятой части красит место целиком. */
    @Test
    fun `отказ КГД красит место`() {
        assertEquals(StatusTone.Bad, groupTone(place(registered = 9, refused = 1)))
        assertEquals(StatusTone.Waiting, groupTone(place(registered = 30, refused = 1)))
    }

    /** Заблокированная касса осталась бедой в том же счёте, что и отказ. */
    @Test
    fun `заблокированная касса красит место`() {
        assertEquals(StatusTone.Bad, groupTone(place(registered = 2, blocked = 1)))
    }

    /**
     * Снятая с учёта касса — не поломка.
     *
     * Снятие затеял сам владелец и довёл до конца; красная плашка рядом
     * с работающими кассами читалась как «эту надо чинить».
     */
    @Test
    fun `снятая с учёта касса не красится отказом`() {
        val paints = paints()

        assertEquals(paints.idle, paints.deregistered, "снятая касса покрашена не нейтрально")
        assertNotEquals(paints.refusal, paints.deregistered)
        assertEquals(paints.refusal, paints.refusedCode, "отказ КГД перестал быть отказом")
    }

    /** Кассы одного места: столько-то черновиков, столько-то на учёте и так далее. */
    private fun place(
        draft: Int = 0,
        registered: Int = 0,
        deregistered: Int = 0,
        refused: Int = 0,
        blocked: Int = 0
    ): KkmGroup {
        val kkms = listOf(
            "DRAFT" to draft,
            "REGISTERED" to registered,
            "DEREGISTERED" to deregistered,
            "REGISTRATION_IN_ISNA_ERROR" to refused,
            "REGISTERED" to blocked
        ).flatMapIndexed { kind, (status, count) ->
            (1..count).map { at -> kkm("$kind-$at", status, blocked = kind == BLOCKED_KIND) }
        }
        return kkmGroups(kkms.map { PlacedKkm(it, LATITUDE, LONGITUDE) }, ZOOM).single()
    }

    private fun kkm(id: String, status: String, blocked: Boolean) = AnalyticsKkm(
        cashRegisterId = id,
        kkmId = 2000302,
        status = status,
        blocked = blocked
    )

    /** Цвета плашек состояния, снятые со схемы одним кадром. */
    private fun paints(): Paints {
        var seen = Paints(Color.Unspecified, Color.Unspecified, Color.Unspecified, Color.Unspecified)
        RenderProbe(WIDTH, HEIGHT) {
            seen = Paints(
                deregistered = statusColor("DEREGISTERED"),
                refusedCode = statusColor("REGISTRATION_IN_ISNA_ERROR"),
                idle = toneColor(StatusTone.Idle),
                refusal = StatusColors.refused
            )
        }.use { it.frame() }
        return seen
    }

    private data class Paints(val deregistered: Color, val refusedCode: Color, val idle: Color, val refusal: Color)

    private companion object {
        const val LATITUDE = 43.238949
        const val LONGITUDE = 76.889709
        const val ZOOM = 12

        /** Какая по счёту кучка в наборе места собрана из заблокированных касс. */
        const val BLOCKED_KIND = 4

        const val WIDTH = 200
        const val HEIGHT = 100
    }
}
