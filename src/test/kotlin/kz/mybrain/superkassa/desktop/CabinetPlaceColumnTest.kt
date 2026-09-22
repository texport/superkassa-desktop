package kz.mybrain.superkassa.desktop

import io.ktor.http.HttpStatusCode
import kz.mybrain.superkassa.desktop.ui.cabinet.PlacesPage
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Колонка торговых точек, когда кабинет не отдал списка.
 *
 * Пустой ответ и отказ — разные вещи, и владелец обязан их различать.
 * Колонка говорила на то и другое одно и то же: «Торговых точек нет —
 * заведите первую». Владельцу сети, у которого точек тысяча, экран
 * сообщал, что хозяйства у него нет вовсе, и предлагал завести первую
 * точку — при недоступном кабинете, где это всё равно не получится.
 */
class CabinetPlaceColumnTest {

    /** Раздел точек с кабинетом, отвечающим заданным. */
    private fun column(name: String, reply: (String) -> CabinetReply): ByteArray {
        val stage = CabinetStage(reply)
        return RenderProbe(WIDE, HIGH) { PlacesPage(stage.session, stage.cabinet, stage.texts) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            val frame = probe.frame()
            Look.shot(name, frame)
            frame
        }
    }

    private fun nothing() = CabinetReply("""{"page":0,"size":50,"totalElements":0,"items":[]}""")

    @Test
    fun `отказ кабинета не выдаётся за пустое хозяйство владельца`() {
        val empty = column("audit-cabinet-places-empty") { nothing() }
        val refused = column("audit-cabinet-places-refused") {
            refusal("INTERNAL_ERROR", "Кабинет временно недоступен", HttpStatusCode.ServiceUnavailable)
        }

        assertTrue(
            !empty.contentEquals(refused),
            "отказ кабинета показан теми же словами, что и пустой список точек"
        )
    }

    /** Истёкший доступ — тоже не пустота: он возвращает владельца ко входу. */
    @Test
    fun `истёкший доступ не выдаётся за пустое хозяйство владельца`() {
        val empty = column("audit-cabinet-places-empty-2") { nothing() }
        val expired = column("audit-cabinet-places-expired") {
            refusal("UNAUTHORIZED", "Срок доступа истёк", HttpStatusCode.Unauthorized)
        }

        assertTrue(
            !empty.contentEquals(expired),
            "истёкший доступ показан теми же словами, что и пустой список точек"
        )
    }

    private companion object {
        const val WIDE = 1180
        const val HIGH = 820
        const val SETTLE = 40
    }
}
