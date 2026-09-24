package kz.mybrain.superkassa.presentation.users

import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.tap
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Удалив себя, администратор выходит, а не остаётся за кассой с мёртвым пином.
 *
 * Второй администратор на кассе есть, и удалить себя правила не мешают —
 * так уходит из компании тот, кто её заводил. Но касса вместе с кассиром
 * забывает и его пин: рабочее место продолжало показывать вошедшего
 * и отвечало отказом на каждое следующее действие, ни разу не сказав,
 * что кассира больше нет.
 *
 * Корзина ищется по подписи в строке своего кассира, «Удалить» — по надписи.
 */
class UsersRemoveSelfTest {
    private val texts = textsOf(Language.Ru).common

    @Test
    fun `удаливший себя администратор выходит из кассы`(): Unit = inlineMain {
        val scene = UsersScene(users = listOf(UsersScene.ADMIN, UsersScene.DEPUTY))
        val model = scene.model()
        RenderProbe(width = WIDE, height = TALL) { UsersScreen(model) }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            val remove = probe.inRowOf(UsersScene.ADMIN.name) { it.label == texts.users.delete }
            probe.click(remove.at + Offset(remove.width / 2f, remove.height / 2f))
            repeat(SETTLE) { probe.frame() }
            File("/tmp/audit-users-remove-self-ask.png").writeBytes(probe.frame())
            probe.tap { it.text == texts.users.delete }
            repeat(SETTLE) { probe.frame() }
            File("/tmp/audit-users-remove-self-done.png").writeBytes(probe.frame())

            assertTrue(scene.asked.any { it.startsWith("deleteUser") }, "кассира так и не удалили: ${scene.asked}")
            assertFalse(scene.signIn.state.value.signedIn, "удалённый кассир остался за кассой")
        }
    }

    private companion object {
        const val WIDE = 1400
        const val TALL = 900
        const val SETTLE = 40
    }
}
