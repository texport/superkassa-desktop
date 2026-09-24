package kz.mybrain.superkassa.presentation.analytics.map

import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.designsystem.status.StatusTone
import kz.mybrain.superkassa.designsystem.status.toneColor
import kz.mybrain.superkassa.designsystem.theme.StatusColors
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsKkm
import kz.mybrain.superkassa.domain.analytics.model.PlacedKkm
import kz.mybrain.superkassa.domain.cabinet.model.KkmRecord
import kz.mybrain.superkassa.domain.cabinet.model.kkmRecord
import kz.mybrain.superkassa.presentation.cabinet.statusColor
import kz.mybrain.superkassa.refusal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Одиннадцать состояний учёта — в пять смыслов, и цвет по ним.
 *
 * Приложение спрашивало у состояния одно: на учёте касса или нет, —
 * и черновик попадал в одну кучу с отказом КГД. В кабинете показа
 * из 3294 касс 3288 черновиков, четыре на учёте и две снятые: карта
 * страны от этого краснела целиком. Числа взяты запросом к кабинету
 * на боевых данных и повторены здесь, чтобы тот же счёт не вернулся.
 *
 * Заведённая касса отделена от поданного заявления: сложенные вместе,
 * они говорили о трёх тысячах заявлений в КГД, которых никто не подавал.
 */
class KkmRecordTest {

    @Test
    fun `состояния учёта разведены по смыслу`() {
        assertEquals(KkmRecord.OnRecord, kkmRecord("REGISTERED"))
        assertEquals(KkmRecord.OnRecord, kkmRecord("REGISTERED_REREGISTRATION_SUCCESS"))
        assertEquals(KkmRecord.Entered, kkmRecord("DRAFT"), "по черновику заявление не подавалось")
        listOf(
            "REGISTRATION_IN_ISNA_PROCESS",
            "REREGISTRATION_IN_ISNA_PROCESS",
            "DEREGISTRATION_IN_ISNA_PROCESS"
        ).forEach { assertEquals(KkmRecord.Applied, kkmRecord(it), "«$it» — заявление подано") }
        assertEquals(KkmRecord.Refused, kkmRecord("REGISTRATION_IN_ISNA_ERROR"), "в постановке на учёт отказано")
        assertEquals(KkmRecord.Deregistered, kkmRecord("DEREGISTERED"))
    }

    /**
     * Отказ в перерегистрации и в снятии не снимает кассу с учёта.
     *
     * Касса продолжает работать по закону, отказано только в изменении;
     * считаясь отказом, она выпадала из «на учёте», и владелец не мог
     * ни подать заявление заново, ни сняться с учёта.
     */
    @Test
    fun `отказ в изменении учёта оставляет кассу на учёте`() {
        listOf("REGISTERED_REREGISTRATION_ERROR", "DEREGISTRATION_ERROR").forEach {
            assertEquals(KkmRecord.OnRecord, kkmRecord(it), "«$it» — касса на учёте, отказано в изменении")
        }
    }

    /**
     * Кабинет не назвал состояния вовсе: тревожиться не о чем, но и учёта нет.
     *
     * Такая касса считается заведённой, а не поданной: заявления, о котором
     * кабинет не сказал ни слова, у неё может и не быть.
     */
    @Test
    fun `отсутствие кода не считается отказом`() {
        assertEquals(KkmRecord.Entered, kkmRecord(null))
        assertEquals(KkmRecord.Entered, kkmRecord("  "))
    }

    /**
     * «Нет сведений» — не отказ, каким бы словом оно ни пришло.
     *
     * `UNKNOWN` — это и есть отсутствие кода, написанное словом: тем же
     * словом кабинет отвечает о смене кассы, которой он не знает —
     * в ответе по карте его носят 3288 черновиков сети показа. Считаясь
     * отказом, такая касса вставала в список отказов КГД с советом
     * разобрать причину и подать заявление заново, а её место на карте
     * краснело — из-за отказа, которого не было.
     */
    @Test
    fun `слово «нет сведений» отказом не считается`() {
        assertEquals(KkmRecord.Entered, kkmRecord("UNKNOWN"))
        assertEquals(KkmRecord.Entered, kkmRecord("unknown"))
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
