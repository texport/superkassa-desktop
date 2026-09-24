package kz.mybrain.superkassa.kassa

import io.github.texport.superkassa.testing.api.kassa.TestBench
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.ask
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.login.loginModel
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Касса в процессе приложения — настоящее ядро на временном каталоге,
 * БФД — тестовый из оснастки ядра: касса заводится и работает без сети.
 *
 * Каталог рабочего места не трогается: у каждой проверки свой временный.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EmbeddedKassaTest {
    private val directory: File = createTempDirectory("kassa-").toFile()
    private lateinit var bench: TestBench

    @BeforeTest
    fun open() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        bench = appBench(directory)
    }

    @AfterTest
    fun close() {
        bench.close()
        directory.deleteRecursively()
        Dispatchers.resetMain()
    }

    private fun app(notices: Notices = Notices(), signIn: SignIn = SignIn()) =
        CoreScene.app(EmbeddedKassa(bench.api, Dispatchers.Unconfined), signIn, notices)

    @Test
    fun `на чистом каталоге список касс прочитан и пуст`() {
        val model = loginModel(app())

        model.reload()

        val state = model.state.value
        assertTrue(state.answered && state.listRead)
        assertTrue(state.kkms.isEmpty())
        assertTrue(File(directory, "superkassa.db").isFile, "база ядра не заведена в каталоге кассы")
    }

    @Test
    fun `вход на незаведённую кассу — отказ ядра его кодом и словами`() {
        val notices = Notices()
        val signIn = SignIn()
        val model = loginModel(app(notices, signIn))
        model.reload()
        model.typePin("4821")

        val answer = runBlocking { app().kassa.ask { it.authenticate("no-such-kkm", "4821") } }

        val refused = assertIs<Answer.Refused>(answer)
        assertEquals("KKM_NOT_FOUND", refused.code)
        assertTrue(refused.ru.isNotBlank() && refused.kk.isNotBlank() && refused.en.isNotBlank())
        model.enter()
        assertFalse(signIn.state.value.signedIn, "вход без кассы в списке начат")
    }

    @Test
    fun `заведённая касса в списке входа, и её пином кассир входит`() {
        val kassa = bench.registerKassa(appKassa(adminPin = "7391", cashierPin = "4826", name = "Касса у входа"))
        val signIn = SignIn()
        val model = loginModel(app(signIn = signIn))
        model.reload()
        model.pick(model.state.value.kkms.single())
        model.typePin(kassa.cashierPin)

        model.enter()

        assertEquals(kassa.kkmId, signIn.state.value.kkm?.kkmId)
        assertEquals("Касса у входа", signIn.state.value.kkm?.name)
        assertTrue(signIn.state.value.signedIn, "пин кассира, заданный при заведении, не пустил")
    }

    /** Второй экземпляр на том же каталоге не открывается: у кассы один владелец. */
    @Test
    fun `второй экземпляр на занятом каталоге отказывает`() {
        assertFailsWith<IllegalStateException> { appBench(directory) }
    }

    @Test
    fun `сбой — не отказ, а отмена — не итог`(): Unit = runBlocking {
        val kassa = FakeCore().apply {
            on("getKkm") { error("disk is full") }
            on("listVatRates") { throw CancellationException("screen closed") }
        }.kassa()

        assertEquals(Answer.Failed("IllegalStateException"), kassa.ask { it.getKkm("kkm-1") })
        assertFailsWith<CancellationException> { kassa.ask { it.listVatRates() } }
    }
}
