package kz.mybrain.superkassa.presentation.users.signin

import androidx.compose.runtime.Composable
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.LoginScene
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.users.UsersActions
import kz.mybrain.superkassa.presentation.users.UsersContent
import kz.mybrain.superkassa.presentation.users.UsersScene
import kz.mybrain.superkassa.presentation.users.UsersUiState
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Вход и выбор кассы во всех своих состояниях, включая отказные.
 *
 * Касса без касс и касса, список которой прочитать не удалось, — разные
 * состояния, и на экране они обязаны выглядеть по-разному: во втором
 * случае утверждать, что касс нет, приложению нечем.
 *
 * Снимки остаются в `/tmp/kassa-login-*.png` и `/tmp/kassa-users-*.png`:
 * по ним видно, объяснён ли отказ словами, видно ли главное действие
 * и не обрублена ли строка кассы. Проверка же следит за тем, что случаи
 * собираются и отличаются друг от друга: одинаково непонятная картинка
 * на пустом списке и на молчащей кассе — это дефект, а не состояние.
 */
class KassaLoginLookTest {

    /** Отказ кассы, которым кончилось последнее действие на входе: код и слова кассира. */
    private data class Refusal(val code: String, val words: String)

    /**
     * Вход в том самом окне, каким его собирает приложение.
     *
     * Своя оснастка здесь была бы негодной: окно до входа само кладёт
     * полосу пина, снекбар и полоску занятости, и проверка, собравшая
     * каркас по-своему, проверяла бы оснастку, а не приложение.
     */
    @Composable
    private fun Door(app: AppContainer) = LoginScene.Door(app)

    /**
     * @param kkms кассы, которые отдаёт касса процесса; `null` — список она
     *   не отдаёт, и о кассах неизвестно ничего.
     * @param refusal отказ, которым кончилось последнее действие на входе.
     */
    private fun door(kkms: List<KkmResponse>?, refusal: Refusal? = null): AppContainer {
        val app = CoreScene.app(LoginScene.core(kkms))
        refusal?.let { app.notices.show(Message.Refusal(it.words, it.code)) }
        return app
    }

    @Test
    fun `экран входа собирается во всех состояниях и они различимы`() {
        // Сцены собираются до кадра: вызов внутри его содержимого повторяется
        // на каждой перерисовке, и состояние доставалось каждый раз новому
        // окну — снимки выходили то с содержимым, то без.
        val empty = door(emptyList())
        val silent = door(null)
        val wrongPin = door(listOf(KassaScene.kkm()), refusal = WRONG_PIN)
        val locked = door(listOf(KassaScene.kkm()), refusal = LOCKED_CASHIER)
        val blocked = door(listOf(KassaScene.kkm(state = "BLOCKED")))
        val deregistered = door(listOf(KassaScene.kkm(state = "BLOCKED", kgd = null)))
        val busyShift = door(listOf(KassaScene.kkm()), refusal = SHIFT_OF_ANOTHER)

        val frames = inlineMain {
            mapOf(
                "empty" to KassaScene.shot("login-empty") { Door(empty) },
                "kassa-silent" to KassaScene.shot("login-kassa-silent") { Door(silent) },
                "wrong-pin" to KassaScene.shot("login-wrong-pin") { Door(wrongPin) },
                "locked-cashier" to KassaScene.shot("login-locked-cashier") { Door(locked) },
                "kkm-blocked" to KassaScene.shot("login-kkm-blocked") { Door(blocked) },
                "kkm-deregistered" to KassaScene.shot("login-kkm-deregistered") { Door(deregistered) },
                "shift-of-another" to KassaScene.shot("login-shift-of-another") { Door(busyShift) }
            )
        }

        frames.forEach { (name, frame) -> assertTrue(frame.isNotEmpty(), "пустой кадр: $name") }
        assertTrue(frames.values.map { it.toList() }.distinct().size == frames.size, "состояния входа неотличимы")
    }

    /**
     * Кассиры кассы: пустой список, список с кассирами и молчащая касса.
     *
     * Пустой список — это не пустота: у кассы без кассиров работать нельзя,
     * и экран обязан сказать, что делать. Отказ кассы — третье состояние,
     * и оно не должно выглядеть пустым списком.
     */
    @Test
    fun `список кассиров рисуется и пустым, и заполненным`() {
        val none = object : UsersActions {}
        val me = UsersScene.ADMIN
        val empty = KassaScene.shot("users-empty") {
            UsersContent(UsersUiState(answered = true, kkmChosen = true, me = me), none)
        }
        val filled = KassaScene.shot("users-two") {
            UsersContent(UsersUiState(users = CASHIERS, answered = true, kkmChosen = true, me = me), none)
        }
        val silent = KassaScene.shot("users-node-silent") {
            UsersContent(UsersUiState(unreadable = true, kkmChosen = true, me = me), none)
        }

        val frames = listOf(empty, filled, silent)
        frames.forEach { assertTrue(it.isNotEmpty()) }
        assertTrue(frames.map { it.toList() }.distinct().size == 3, "состояния списка кассиров неотличимы")
    }

    private companion object {
        val CASHIERS = listOf(UsersScene.ADMIN, UsersScene.CASHIER)

        val WRONG_PIN = Refusal("INVALID_PIN", "Неверный пин")
        val LOCKED_CASHIER = Refusal("USER_BLOCKED", "Кассир заблокирован, позовите администратора")
        val SHIFT_OF_ANOTHER = Refusal("SHIFT_ALREADY_OPEN", "Смена уже открыта другим кассиром")
    }
}
