package kz.mybrain.superkassa.kassa

import io.github.texport.superkassa.embedded.api.Superkassa
import io.github.texport.superkassa.embedded.api.SuperkassaPlatform
import io.github.texport.superkassa.embedded.api.createSuperkassa
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.domain.kassa.Answer
import kz.mybrain.superkassa.domain.kassa.ask
import kz.mybrain.superkassa.domain.signin.SignIn
import kz.mybrain.superkassa.presentation.AppContainer
import kz.mybrain.superkassa.presentation.login.LoginViewModel
import kz.mybrain.superkassa.presentation.messages.Notices
import kz.mybrain.superkassa.presentation.strings.Language
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
 * Касса в процессе приложения — настоящее ядро на временном каталоге.
 *
 * Завести кассу без ОФД ядро не даёт, поэтому здесь то, что проверяется
 * без неё: чистый каталог, отказ ядра его словами и разбор итогов.
 * Каталог рабочего места не трогается: у каждой проверки свой временный.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EmbeddedKassaTest {
    private val directory: File = createTempDirectory("kassa-").toFile()
    private lateinit var core: Superkassa

    @BeforeTest
    fun open() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        core = createSuperkassa(SuperkassaPlatform(directory.path), EmbeddedKassa.config())
    }

    @AfterTest
    fun close() {
        core.close()
        directory.deleteRecursively()
        Dispatchers.resetMain()
    }

    private fun app(notices: Notices = Notices(), signIn: SignIn = SignIn()) = AppContainer(
        kassa = EmbeddedKassa(core.api, Dispatchers.Unconfined),
        signIn = signIn,
        notices = notices,
        memory = MemoryWorkplace(),
        journal = SilentJournal,
        language = { Language.Ru }
    )

    @Test
    fun `на чистом каталоге список касс прочитан и пуст`() {
        val model = LoginViewModel(app())

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
        val model = LoginViewModel(app(notices, signIn))
        model.reload()
        model.typePin("4821")

        val answer = runBlocking { app().kassa.ask { it.authenticate("no-such-kkm", "4821") } }

        val refused = assertIs<Answer.Refused>(answer)
        assertEquals("KKM_NOT_FOUND", refused.code)
        assertTrue(refused.ru.isNotBlank() && refused.kk.isNotBlank() && refused.en.isNotBlank())
        model.enter()
        assertFalse(signIn.state.value.signedIn, "вход без кассы в списке начат")
    }

    /** Второй экземпляр на том же каталоге не открывается: у кассы один владелец. */
    @Test
    fun `второй экземпляр на занятом каталоге отказывает`() {
        assertFailsWith<IllegalStateException> {
            createSuperkassa(SuperkassaPlatform(directory.path), EmbeddedKassa.config())
        }
    }

    @Test
    fun `сбой — не отказ, а отмена — не итог`() = runBlocking {
        val kassa = FakeCore().apply {
            on("getKkm") { error("disk is full") }
            on("listVatRates") { throw CancellationException("screen closed") }
        }.kassa()

        assertEquals(Answer.Failed("IllegalStateException"), kassa.ask { it.getKkm("kkm-1") })
        assertFailsWith<CancellationException> { kassa.ask { it.listVatRates() } }
        Unit
    }
}
