package kz.mybrain.superkassa

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.data.node.Kkm
import kz.mybrain.superkassa.data.node.KkmUser
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.LoginScene
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.presentation.AppContainer
import kz.mybrain.superkassa.presentation.messages.Message
import kz.mybrain.superkassa.presentation.session.Session
import kz.mybrain.superkassa.presentation.users.UsersScreen
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

    /**
     * Вход в том самом окне, каким его собирает приложение.
     *
     * Своя оснастка здесь была бы негодной: окно до входа само кладёт
     * полосу пина, снекбар и полоску занятости, и проверка, собравшая
     * каркас по-своему, проверяла бы оснастку, а не приложение.
     */
    @Composable
    private fun Door(door: Pair<Session, AppContainer>) = LoginScene.Door(door.first, door.second)

    /**
     * @param kkms кассы, которые отдаёт касса процесса; `null` — список она
     *   не отдаёт, и о кассах неизвестно ничего.
     * @param refusal отказ, которым кончилось последнее действие на входе.
     */
    private fun door(folder: String, kkms: List<Kkm>?, refusal: NodeRefusal? = null): Pair<Session, AppContainer> {
        val session = KassaScene.session(folder, kkm = null)
        refusal?.let { session.notices.show(Message.Refusal(it.ru, it.code)) }
        return session to CoreScene.app(session, LoginScene.core(kkms?.map(CoreScene::of)))
    }

    @Test
    fun `экран входа собирается во всех состояниях и они различимы`() {
        // Сцены собираются до кадра: вызов внутри его содержимого повторяется
        // на каждой перерисовке, и состояние доставалось каждый раз новому
        // сеансу — снимки выходили то с содержимым, то без.
        val empty = door("login-empty", emptyList())
        val silent = door("login-silent", null)
        val wrongPin = door("login-pin", listOf(KassaScene.kkm()), refusal = WRONG_PIN)
        val locked = door("login-locked", listOf(KassaScene.kkm()), refusal = LOCKED_CASHIER)
        val blocked = door("login-blocked", listOf(KassaScene.kkm(state = "BLOCKED")))
        val deregistered = door("login-dereg", listOf(KassaScene.kkm(state = "BLOCKED", kgd = null)))
        val busyShift = door("login-another", listOf(KassaScene.kkm()), refusal = SHIFT_OF_ANOTHER)

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
     * Кассиры кассы: пустой список, список с кассирами и молчащий узел.
     *
     * Пустой список — это не пустота: у кассы без кассиров работать нельзя,
     * и экран обязан сказать, что делать. Отказ узла — третье состояние,
     * и оно не должно выглядеть пустым списком.
     */
    @Test
    fun `список кассиров рисуется и пустым, и заполненным`() {
        val noCashiers = KassaScene.session("users-empty", cashiers = emptyList())
        val twoCashiers = KassaScene.session("users-two", cashiers = CASHIERS)
        val silentNode = KassaScene.session("users-silent", available = false)

        val empty = KassaScene.shot("users-empty") { UsersScreen(noCashiers) }
        val filled = KassaScene.shot("users-two") { UsersScreen(twoCashiers) }
        val silent = KassaScene.shot("users-node-silent") { UsersScreen(silentNode) }

        val frames = listOf(empty, filled, silent)
        frames.forEach { assertTrue(it.isNotEmpty()) }
        assertTrue(frames.map { it.toList() }.distinct().size == 3, "состояния списка кассиров неотличимы")
    }

    private companion object {
        val CASHIERS = listOf(
            KkmUser(userId = "u-1", name = "Айгүл Сәрсенова", role = "ADMIN"),
            KkmUser(userId = "u-2", name = "Дана Жумабаева", role = "CASHIER")
        )

        val WRONG_PIN = NodeRefusal("INVALID_PIN", "Неверный пин", "Пин дұрыс емес", "Wrong pin")
        val LOCKED_CASHIER = NodeRefusal(
            code = "USER_BLOCKED",
            ru = "Кассир заблокирован, позовите администратора",
            kk = "Кассир бұғатталған, әкімшіні шақырыңыз",
            en = "Cashier is blocked, call the administrator"
        )
        val SHIFT_OF_ANOTHER = NodeRefusal(
            code = "SHIFT_ALREADY_OPEN",
            ru = "Смена уже открыта другим кассиром",
            kk = "Ауысым басқа кассирмен ашылған",
            en = "Shift is already open by another cashier"
        )
    }
}
