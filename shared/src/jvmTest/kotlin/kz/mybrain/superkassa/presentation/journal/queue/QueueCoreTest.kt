package kz.mybrain.superkassa.presentation.journal.queue

import io.github.texport.superkassa.testing.api.kassa.ReadyKassa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.domain.journal.model.QueueState
import kz.mybrain.superkassa.domain.journal.model.state
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.appBench
import kz.mybrain.superkassa.kassa.appKassa
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Очередь досылки на настоящем ядре и тестовом БФД, как у администратора:
 * чек без связи, неудачная досылка, повтор — только при закрытой смене
 * в режиме программирования, как того требует касса.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QueueCoreTest {
    private val directory: File = createTempDirectory("kassa-queue-").toFile()
    private val bench = appBench(directory)
    private val kassa: ReadyKassa = bench.registerKassa(appKassa(adminPin = ADMIN, cashierPin = CASHIER))
    private val notices = Notices()
    private val signIn = SignIn()
    private val texts = textsOf(Language.Ru).journal.queue

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun close() {
        Dispatchers.resetMain()
        bench.close()
        directory.deleteRecursively()
    }

    private fun model(): QueueViewModel {
        signIn.enter(kassa.info(), bench.api.authenticate(kassa.kkmId, ADMIN), ADMIN)
        return queueModel(CoreScene.app(EmbeddedKassa(bench.api, Dispatchers.Unconfined), signIn, notices))
            .also { it.refresh() }
    }

    /** Чек без связи, досылка которого не удалась: задача ждёт повтора руками. */
    private fun failedSale() {
        kassa.openShift()
        kassa.offlineSale()
        bench.bfd.unreachableOnce()
        kassa.resendQueue()
    }

    /** Режим кассы сменён в настройках: касса ответила о себе, и это видят все разделы. */
    private fun programming(on: Boolean) {
        val kkm = if (on) {
            bench.api.enterProgramming(kassa.kkmId, ADMIN)
        } else {
            bench.api.exitProgramming(kassa.kkmId, ADMIN)
        }
        signIn.refresh(kkm)
    }

    @Test
    fun `чек без связи ждёт в очереди, после досылки — отправлен`() {
        kassa.openShift()
        kassa.offlineSale()
        val model = model()
        assertEquals(1, model.state.value.waiting.size)

        kassa.resendQueue()
        model.refresh()

        assertTrue(model.state.value.waiting.isEmpty())
        assertEquals(1, model.state.value.sent.size)
    }

    @Test
    fun `неудачная досылка видна с причиной, повтор вне режима программирования не предложен`() {
        failedSale()
        val model = model()

        val failed = model.state.value.failed.single()
        assertEquals(QueueState.Failed, failed.state)
        assertTrue(!failed.errorRu.isNullOrBlank(), "причина неудачи не названа")
        assertFalse(model.state.value.canRetry)
    }

    @Test
    fun `в режиме программирования при открытой смене повтор не предложен, и сказано закрыть смену`() {
        failedSale()
        programming(on = true)
        val model = model()

        assertFalse(model.state.value.canRetry, "повтор предложен при открытой смене — касса ему откажет")
        assertEquals(texts.retryNeedsClosedShift, summaryNote(model.state.value, texts))
        model.retryFailed()
        assertFalse(notices.last is Message.Refusal, "касса отказала в повторе: ${notices.last}")
    }

    @Test
    fun `смена закрыта, режим программирования — повтор ставит задачу заново, и она уходит`() {
        failedSale()
        kassa.closeShift()
        programming(on = true)
        val model = model()
        assertTrue(model.state.value.canRetry)

        model.retryFailed()

        assertIs<Message.Done>(notices.last, "повтор не объявлен: ${notices.last}")
        assertTrue(model.state.value.failed.isEmpty(), "задача осталась неудачной")
        programming(on = false)
        kassa.resendQueue()
        model.refresh()
        assertTrue(model.state.value.waiting.isEmpty() && model.state.value.sent.isNotEmpty())
    }

    private companion object {
        const val ADMIN = "7391"
        const val CASHIER = "4826"
    }
}
