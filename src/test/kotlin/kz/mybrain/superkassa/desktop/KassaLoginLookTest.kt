package kz.mybrain.superkassa.desktop

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.KkmUser
import kz.mybrain.superkassa.desktop.ui.MessageEffect
import kz.mybrain.superkassa.desktop.ui.MessageHost
import kz.mybrain.superkassa.desktop.ui.login.LoginScreen
import kz.mybrain.superkassa.desktop.ui.users.UsersScreen
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Вход и выбор кассы во всех своих состояниях, включая отказные.
 *
 * Снимки остаются в `/tmp/kassa-login-*.png` и `/tmp/kassa-users-*.png`:
 * по ним видно, объяснён ли отказ словами, видно ли главное действие
 * и не обрублена ли строка кассы. Проверка же следит за тем, что случаи
 * собираются и отличаются друг от друга: одинаково непонятная картинка
 * на пустом списке и на молчащем узле — это дефект, а не состояние.
 */
class KassaLoginLookTest {

    /** Экран входа в том же окне, что у приложения: со снекбаром отказов. */
    @Composable
    private fun Door(session: Session) {
        val host = remember { SnackbarHostState() }
        Scaffold(snackbarHost = { MessageHost(host) }) {
            MessageEffect(session.lastMessage, host) {}
            LoginScreen(session, remember { CabinetSession() })
        }
    }

    private fun doorSession(
        folder: String,
        kkms: List<Kkm>,
        available: Boolean = true,
        refusal: NodeRefusal? = null
    ): Session = KassaScene.session(folder, kkm = null, available = available, refusal = refusal).also {
        it.kkms.addAll(kkms)
    }

    @Test
    fun `экран входа собирается во всех состояниях и они различимы`() {
        // Сеансы создаются до сцены: вызов внутри её содержимого повторяется
        // на каждой перерисовке, и состояние доставалось каждый раз новому
        // сеансу — снимки выходили то с содержимым, то без.
        val empty = doorSession("login-empty", emptyList())
        val silent = doorSession("login-silent", emptyList(), available = false)
        val wrongPin = doorSession("login-pin", listOf(KassaScene.kkm()), refusal = WRONG_PIN)
        val locked = doorSession("login-locked", listOf(KassaScene.kkm()), refusal = LOCKED_CASHIER)
        val blocked = doorSession("login-blocked", listOf(KassaScene.kkm(state = "BLOCKED")))
        val deregistered = doorSession("login-dereg", listOf(KassaScene.kkm(state = "BLOCKED", kgd = null)))
        val busyShift = doorSession("login-another", listOf(KassaScene.kkm()), refusal = SHIFT_OF_ANOTHER)

        val frames = mapOf(
            "empty" to KassaScene.shot("login-empty") { Door(empty) },
            "node-silent" to KassaScene.shot("login-node-silent") { Door(silent) },
            "wrong-pin" to KassaScene.shot("login-wrong-pin") { Door(wrongPin) },
            "locked-cashier" to KassaScene.shot("login-locked-cashier") { Door(locked) },
            "kkm-blocked" to KassaScene.shot("login-kkm-blocked") { Door(blocked) },
            "kkm-deregistered" to KassaScene.shot("login-kkm-deregistered") { Door(deregistered) },
            "shift-of-another" to KassaScene.shot("login-shift-of-another") { Door(busyShift) }
        )

        frames.forEach { (name, frame) -> assertTrue(frame.isNotEmpty(), "пустой кадр: $name") }
        assertTrue(
            frames.values.map { it.toList() }.distinct().size == frames.size,
            "состояния экрана входа неотличимы друг от друга"
        )
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
