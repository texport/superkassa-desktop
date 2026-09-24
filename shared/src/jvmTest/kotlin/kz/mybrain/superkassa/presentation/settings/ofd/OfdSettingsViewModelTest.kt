package kz.mybrain.superkassa.presentation.settings.ofd

import io.github.texport.superkassa.core.presentation.api.model.ofd.OfdCommandResponse
import io.github.texport.superkassa.core.presentation.api.model.ofd.OfdCommandStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Связь кассы с БФД без окна: сверка, токен и проверки.
 *
 * Касса отвечает итогом команды и тогда, когда БФД её не выполнил: такой
 * итог — отказ, и «обновлено» поверх него было бы неправдой.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfdSettingsViewModelTest {

    private val core = FakeCore()
    private val signIn = SignIn()
    private val notices = Notices()
    private val app = CoreScene.app(core, signIn, notices)
    private val money = textsOf(Language.Ru).kassa.money.kkm
    private val texts = textsOf(Language.Ru).common.settings

    @BeforeTest
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        core.on("getKkm") { CoreScene.kkm(name = "Касса после сверки") }
        signIn.enter(CoreScene.kkm(), CoreScene.cashier(), CoreScene.PIN)
    }

    @AfterTest
    fun reset() = Dispatchers.resetMain()

    @Test
    fun `сверка сведений перечитывает кассу и объявляет итог`() {
        core.on("syncOfdServiceInfo") { OfdCommandResponse(status = OfdCommandStatus.OK) }
        val model = ofdSettingsModel(app.services)

        model.syncService()

        assertEquals("Касса после сверки", signIn.state.value.kkm?.name, "касса не перечитана после сверки")
        assertEquals(Message.Done(money.syncServiceDone), notices.last)
    }

    @Test
    fun `невыполненная БФД сверка — отказ его словами`() {
        core.on("syncOfdCounters") {
            OfdCommandResponse(OfdCommandStatus.FAILED, resultCode = 11, errorMessage = "Касса заблокирована в ОФД")
        }
        val model = ofdSettingsModel(app.services)

        model.syncCounters()

        assertEquals(Message.Refusal("Касса заблокирована в ОФД", "OFD_11"), notices.last)
    }

    /** Касса молчит — сверка не выполнена, и так и сказано. */
    @Test
    fun `сбой кассы при сверке назван сбоем`() {
        core.on("syncOfdCounters") { error("socket closed") }
        val model = ofdSettingsModel(app.services)

        model.syncCounters()

        assertIs<Message.Failed>(notices.last)
    }

    @Test
    fun `сверку нельзя начать, пока очередь не пуста`() {
        signIn.refresh(CoreScene.kkm().copy(offlineQueueCount = 3))
        val model = ofdSettingsModel(app.services)

        assertFalse(model.state.value.syncable)
        assertFalse(model.state.value.serviceSyncable)
    }

    @Test
    fun `новый токен набирается цифрами и уходит в кассу`() {
        signIn.refresh(CoreScene.kkm(state = "PROGRAMMING"))
        var sent = ""
        core.on("updateOfdToken") { args ->
            sent = args[2] as String
            true
        }
        val model = ofdSettingsModel(app.services)

        model.typeToken("32 95-18")
        model.saveToken()

        assertEquals("329518", sent)
        assertEquals("", model.state.value.token, "токен остался в поле после замены")
        assertEquals(Message.Done(texts.tokenSaved), notices.last)
    }

    @Test
    fun `проверка связи и сведения ОФД остаются на карточке`() {
        core.on("checkOfdConnection") { OfdCommandResponse(status = OfdCommandStatus.OK) }
        core.on("getOfdInfo") {
            val answer = buildJsonObject { put("protocolVersion", JsonPrimitive("204")) }
            OfdCommandResponse(status = OfdCommandStatus.OK, responseJson = answer)
        }
        val model = ofdSettingsModel(app.services)

        model.checkLink()
        model.askInfo()

        assertTrue(model.state.value.linkAlive == true)
        assertEquals("204", model.state.value.summary?.protocol)
    }
}
