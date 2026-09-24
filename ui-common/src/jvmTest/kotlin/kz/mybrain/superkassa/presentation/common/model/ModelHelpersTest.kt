package kz.mybrain.superkassa.presentation.common.model

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.SilentJournal
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.strings.api.Language
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Общий помощник моделей без экрана: строка сообщений, занятость, слежение за входом.
 *
 * Правило строки: удача снимает только отказ того же обращения, а занятость —
 * счётчик, и второе нажатие во время работы ничего не начинает.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ModelHelpersTest {
    private val notices = Notices()
    private val talk = Talk(notices, SilentJournal) { Language.Ru }
    private val refused = Answer.Refused(
        code = "KKM_BLOCKED",
        ru = "Касса заблокирована",
        kk = "Касса бұғатталған",
        en = "Register is blocked"
    )

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    @Test
    fun `отказ показывается словами кассы, а удача того же обращения его снимает`() {
        talk.shown(refused, "Сохранить", "save")
        assertEquals(Message.Refusal("Касса заблокирована", "KKM_BLOCKED"), notices.last)

        talk.shown(Answer.Done(Unit), "Сохранить", "save")

        assertNull(notices.last)
    }

    @Test
    fun `удача соседнего обращения чужой отказ не снимает`() {
        talk.shown(refused, "Сохранить", "save")

        talk.shown(Answer.Done(Unit), "Прочитать", "read")

        assertEquals(Message.Refusal("Касса заблокирована", "KKM_BLOCKED"), notices.last)
    }

    @Test
    fun `второе нажатие во время работы ничего не начинает`() {
        val model = object : ViewModel() {}
        val busy = Busy()
        val gate = CompletableDeferred<Unit>()
        var started = 0

        model.whileBusy(busy) {
            started++
            gate.await()
        }
        val second = model.whileBusy(busy) { started++ }

        assertNull(second)
        assertTrue(busy.now)
        gate.complete(Unit)
        assertFalse(busy.now)
        assertEquals(1, started)
    }

    @Test
    fun `экран начинает заново только при другом кассире или кассе`() {
        val signIn = SignIn()
        val model = object : ViewModel() {}
        val seats = mutableListOf<String?>()
        model.followSeat(signIn.state) { seats += it.cashier?.userId }

        signIn.enter(CoreScene.kkm(), CoreScene.cashier(), CoreScene.PIN)
        signIn.refresh(CoreScene.kkm(name = "Касса у окна"))
        signIn.signOut()

        assertEquals(listOf(null, "u-1", null), seats)
    }
}
