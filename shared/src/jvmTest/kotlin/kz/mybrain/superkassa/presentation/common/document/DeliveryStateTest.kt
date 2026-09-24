package kz.mybrain.superkassa.presentation.common.document

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.domain.document.model.DeliveryCodes
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.common.stringsOf
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Состояние доставки на экране кассира: словами, а не кодом кассы.
 *
 * Касса называет доставку двумя наборами кодов: журнал документов отдаёт
 * `SENT`, `FAILED` и `INTERNAL`, а справочник состояний отправки —
 * `ONLINE_OK`, `ONLINE_ERROR`, `OFFLINE_QUEUED` и `NOT_SENT`. Поле
 * состояния объявлено свободной строкой, и плашка
 * выводила незнакомый код как есть: в строке документа смены у кассира
 * стояло `ONLINE_OK`, хотя плашка объявлена словами кассира.
 */
class DeliveryStateTest {

    private fun document(status: String?, autonomous: Boolean = false, type: String = "SALE") =
        CoreScene.document("d-1", type = type, status = status).copy(isAutonomous = autonomous)

    @Test
    fun `оба набора кодов кассы разобраны одними и теми же словами`() {
        assertEquals(JournalDelivery.Delivered, deliveryOf(document("SENT")))
        assertEquals(JournalDelivery.Delivered, deliveryOf(document("ONLINE_OK")))
        assertEquals(JournalDelivery.Refused, deliveryOf(document("FAILED")))
        assertEquals(JournalDelivery.Refused, deliveryOf(document("ONLINE_ERROR")))
        assertEquals(JournalDelivery.Queued, deliveryOf(document("PENDING")))
        assertEquals(JournalDelivery.Queued, deliveryOf(document("OFFLINE_QUEUED")))
        assertEquals(JournalDelivery.Queued, deliveryOf(document("NOT_SENT")))
        assertEquals(JournalDelivery.Internal, deliveryOf(document("INTERNAL")))
        assertEquals(JournalDelivery.Resent, deliveryOf(document("ONLINE_OK", autonomous = true)))
        assertEquals(
            JournalDelivery.Internal,
            deliveryOf(document("SENT", type = "SHIFT_OPEN")),
            "открытие смены в БФД не уходит вовсе, чем бы касса его ни отметила"
        )
    }

    @Test
    fun `незнакомый код становится состоянием без имени кода`() {
        assertEquals(JournalDelivery.Unknown, deliveryOf(document("DELIVERED")))
        assertNull(deliveryOf(document(null)), "касса о доставке не сказала ничего — состояния нет")
        val words = Language.entries.associateWith { JournalDelivery.Unknown.title(stringsOf(it).status) }
        words.forEach { (language, text) ->
            assertTrue(text.isNotBlank(), "на языке $language состояние не названо")
            assertFalse(
                text in PROTOCOL_CODES,
                "на языке $language вместо надписи стоит код кассы: $text"
            )
        }
        assertEquals(
            Language.entries.size,
            words.values.distinct().size,
            "надпись повторяется дословно: значит один из языков остался непереведённым"
        )
    }

    /**
     * Два разных незнакомых кода дают на экране одну и ту же надпись.
     *
     * Так видно, что до кадра доходит не код: пока плашка печатала
     * пришедшее как есть, кадры с `ZZZ_ONE` и `QQQ_TWO` отличались друг
     * от друга — то есть кассир читал в строке документа протокол.
     */
    @Test
    fun `незнакомый код не доходит до кадра кодом`() {
        val first = chip("ZZZ_ONE")
        val second = chip("QQQ_TWO")
        val silent = chip(null)

        assertTrue(first.contentEquals(second), "разные незнакомые коды нарисованы по-разному: на плашке код кассы")
        assertFalse(
            first.contentEquals(silent),
            "молчание кассы и незнакомый код выглядят одинаково: одно из двух состояний экран не называет"
        )
    }

    /** Кадр одной плашки; остаётся в `/tmp/loose-ends-delivery-*.png`, чтобы смотреть глазами. */
    private fun chip(status: String?): ByteArray {
        val frame = RenderProbe(width = CHIP_WIDE, height = CHIP_TALL) {
            Box(modifier = Modifier.fillMaxSize().padding(Spacing.roomy)) {
                DocumentDeliveryChip(document(status))
            }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.frame()
        }
        File("/tmp/loose-ends-delivery-${status ?: "silent"}.png").writeBytes(frame)
        return frame
    }

    private companion object {
        const val CHIP_WIDE = 320
        const val CHIP_TALL = 80
        const val SETTLE = 20

        /** Всё, чем касса отмечает доставку: ни одно из этих слов кассиру не показывают. */
        val PROTOCOL_CODES = DeliveryCodes.delivered + DeliveryCodes.refused +
            DeliveryCodes.queued + DeliveryCodes.internal
    }
}
