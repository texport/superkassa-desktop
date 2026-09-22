package kz.mybrain.superkassa.desktop

import androidx.compose.material3.SnackbarDuration
import kz.mybrain.superkassa.desktop.app.Message
import kz.mybrain.superkassa.desktop.ui.durationOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * Сообщение снимается само.
 *
 * Отказ узла стоял, пока кассир не нажмёт «Скрыть»: снекбар лежит
 * поверх содержимого и закрывал нижний край экрана — а нажимают его
 * не сразу и не всегда.
 */
class MessageDurationTest {

    private val messages = listOf(
        Message.Done("чек пробит"),
        Message.Refusal("смена открыта", "KKM_TAX_SETTINGS_SHIFT_OPEN"),
        Message.NodeUnavailable("налоги кассы"),
        Message.NoAnswer("налоги кассы")
    )

    @Test
    fun `ни одно сообщение не остаётся на экране навсегда`() {
        messages.forEach { message ->
            assertNotEquals(
                SnackbarDuration.Indefinite,
                durationOf(message),
                "$message висит, пока его не закроют"
            )
        }
    }

    @Test
    fun `отказ стоит дольше удачи`() {
        assertEquals(SnackbarDuration.Short, durationOf(messages.first()))
        messages.drop(1).forEach {
            assertEquals(SnackbarDuration.Long, durationOf(it), "$it гаснет так же быстро, как удача")
        }
    }
}
