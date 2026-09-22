package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.ui.login.EmptyKkms
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.stringsOf
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Пустой список касс и молчащий узел — разные беды.
 *
 * Экран входа встречал обе одной картинкой: «На этом узле ни одной кассы»
 * с «Новой кассой» главным действием. На молчащем узле это утверждение
 * о том, чего приложение не знает, а предложенное действие ведёт заводить
 * кассу там, где не читается даже список. Плашка в углу при этом писала
 * «Узел недоступен» — экран спорил сам с собой.
 *
 * Кадры остаются в `/tmp/login-empty-*.png`: по ним видно, что стоит
 * главным действием.
 */
class LoginEmptyTest {

    @Composable
    private fun Empty(listRead: Boolean) {
        EmptyKkms(
            listRead = listRead,
            onReload = {},
            onCabinet = {},
            onRegister = {},
            onSettings = {},
        )
    }

    private fun shot(name: String, listRead: Boolean): ByteArray {
        val frame = RenderProbe(width = WIDTH, height = HEIGHT) {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().padding(Spacing.roomy)) {
                Empty(listRead)
            }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            probe.frame()
        }
        File("/tmp/login-empty-$name.png").writeBytes(frame)
        return frame
    }

    @Test
    fun `молчащий узел не выдаётся за узел без касс`() {
        val answered = shot("answered", listRead = true)
        val silent = shot("silent", listRead = false)

        assertFalse(
            answered.contentEquals(silent),
            "узел без касс и узел, который не ответил, встречают кассира одним и тем же экраном"
        )
    }

    /** Слова у двух бед свои на каждом языке: перевод не свёлся к одному. */
    @Test
    fun `о молчании узла сказано своими словами на каждом языке`() {
        Language.entries.forEach { language ->
            val texts = stringsOf(language).login
            assertTrue(texts.kkmsUnreadTitle != texts.noKkmsTitle, "$language: название беды одно на оба случая")
            assertTrue(texts.kkmsUnread != texts.noKkms, "$language: объяснение одно на оба случая")
        }
    }

    private companion object {
        const val WIDTH = 760
        const val HEIGHT = 520
        const val SETTLE = 12
    }
}
