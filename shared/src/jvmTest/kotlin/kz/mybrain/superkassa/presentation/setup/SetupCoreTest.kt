package kz.mybrain.superkassa.presentation.setup

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmListParams
import io.github.texport.superkassa.core.presentation.api.model.user.UserRole
import io.github.texport.superkassa.testing.api.bfd.FakeBfd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.data.kassa.EmbeddedKassa
import kz.mybrain.superkassa.domain.setup.port.FakeSetupCabinet
import kz.mybrain.superkassa.domain.setup.port.SetupPorts
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.MemorySetup
import kz.mybrain.superkassa.kassa.appBench
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.setup.setupTexts
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Мастер подключения на настоящем ядре и тестовом БФД: заводской номер
 * выдаёт касса, шаги кабинета отвечает подменный кабинет, кассу заводит
 * ядро — и без связи с БФД отказывает, не оставляя полузаведённой кассы.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SetupCoreTest {
    private val directory: File = createTempDirectory("kassa-setup-").toFile()
    private val bench = appBench(directory)
    private val notices = Notices()
    private val memory = MemorySetup()
    private val cabinet = FakeSetupCabinet(token = FakeBfd.FIRST_TOKEN.toString())
    private val texts = setupTexts(Language.Ru)
    private var done = 0

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun close() {
        Dispatchers.resetMain()
        bench.close()
        directory.deleteRecursively()
    }

    private fun model(): SetupViewModel {
        val app = CoreScene.app(EmbeddedKassa(bench.api, Dispatchers.Unconfined), notices = notices)
        return setupModel(app, SetupPorts(memory, cabinet), DirectCalls()).also { it.reload() }
    }

    private fun SetupViewModel.byHand() {
        chooseWay(SetupWay.ByHand)
        val token = FakeBfd.FIRST_TOKEN.toString()
        edit(KkmForm(systemId = SYSTEM_ID, token = token, adminPin = PIN, adminPinRepeat = PIN))
    }

    private fun kkms(): Int = bench.api.listKkms(KkmListParams()).total

    @Test
    fun `вручную без связи с БФД — отказ ядра, касса не заведена, набранное на месте`() {
        val model = model()
        model.byHand()
        bench.bfd.disconnect()

        model.connect { done++ }

        val refusal = assertIs<Message.Refusal>(notices.last, "отказа нет: ${notices.last}")
        assertEquals("OFD_COMMAND_FAILED", refusal.code)
        assertEquals(0, kkms(), "касса заведена без ответа БФД")
        assertEquals(0, done, "мастер закрыт отказом")
        assertEquals(SYSTEM_ID, model.state.value.form.systemId, "набранное стёрто отказом")
        assertFalse(model.state.value.form.busy)
    }

    @Test
    fun `связь вернулась — та же форма заводит кассу, и администратор входит своим пином`() {
        val model = model()
        model.byHand()
        bench.bfd.disconnect()
        model.connect { done++ }
        bench.bfd.connect()

        model.connect { done++ }

        assertEquals(Message.Done(texts.connected), notices.last)
        assertEquals(1, done)
        val kkm = bench.api.listKkms(KkmListParams()).items.single()
        assertEquals(UserRole.ADMIN, bench.api.authenticate(kkm.kkmId, PIN).role)
        assertEquals("", model.state.value.byHand.token, "токен остался на экране")
    }

    @Test
    fun `через кабинет — номер от кассы, токен от кабинета, пройденное забыто после заведения`() {
        val model = model()
        model.getFactory()
        val factory = model.state.value.draft.factoryNumber
        assertTrue(!factory.isNullOrBlank(), "касса не выдала заводской номер")

        model.rememberRegister("r-1", SYSTEM_ID.toInt(), "Касса у входа")
        model.edit(KkmForm(adminPin = PIN, adminPinRepeat = PIN))
        model.connect { done++ }

        assertEquals(listOf("token r-1"), cabinet.asked)
        val kkm = bench.api.listKkms(KkmListParams()).items.single()
        assertEquals("Касса у входа", kkm.name)
        assertNull(model.state.value.draft.factoryNumber, "пройденное осталось после заведения")
        assertNull(memory.setupValue("register"))
        assertEquals(1, done)
    }

    @Test
    fun `кабинет токена не выдал — касса не заводится, и владельцу сказано почему`() {
        cabinet.token = null
        val model = model()
        model.getFactory()
        model.rememberRegister("r-1", SYSTEM_ID.toInt(), null)
        model.edit(KkmForm(adminPin = PIN, adminPinRepeat = PIN))

        model.connect { done++ }

        assertEquals(0, kkms())
        assertEquals(0, done)
        assertTrue(model.state.value.draft.cabinetRegisterId == "r-1", "пройденное потеряно без токена")
        val said = assertIs<Message.Refusal>(notices.last, "нажатие «Подключить» прошло молча")
        assertEquals(texts.noToken, said.text)
    }

    private companion object {
        const val SYSTEM_ID = "100001"
        const val PIN = "7391"
    }
}
