package kz.mybrain.superkassa.presentation.print.preview

import io.github.texport.superkassa.core.presentation.api.model.kkm.DocumentDetailsResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.print.model.PrintSource
import kz.mybrain.superkassa.domain.print.model.ShareWay
import kz.mybrain.superkassa.domain.print.port.FakeShareOut
import kz.mybrain.superkassa.domain.print.port.printPorts
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.services
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * «Поделиться чеком»: форма уходит файлом вида из настроек, со ссылкой
 * на электронный чек, — программе, которой покупателю пишут и так.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrintShareTest {
    private val core = FakeCore()
    private val signIn = SignIn()
    private val notices = Notices()
    private val services = CoreScene.services(core, signIn, notices)
    private val texts = textsOf(Language.Ru).common.share

    @BeforeTest
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        signIn.enter(CoreScene.kkm(id = "a1"), CoreScene.cashier(), CoreScene.PIN)
        core.on("getDocumentPrintPdf") { PDF }
    }

    @AfterTest
    fun reset() = Dispatchers.resetMain()

    private fun linked(link: String?) = core.on("getDocumentDetails") {
        DocumentDetailsResponse(CoreScene.document("d-1").copy(receiptUrl = link))
    }

    @Test
    fun `окно «Поделиться» получает PDF, имя файла и ссылку на чек в словах`() {
        linked(LINK)
        val share = FakeShareOut()
        val model = printModel(services, printPorts(share = share))

        model.sharing.share(PrintSource.Journal("d-1"), "receipt-sale-shift-7", ShareWay.System)

        val (receipt, way) = share.shared.single()
        assertEquals(ShareWay.System, way)
        assertEquals("receipt-sale-shift-7.pdf", receipt.name)
        assertEquals("application/pdf", receipt.mime)
        assertTrue(receipt.bytes.contentEquals(PDF), "отдана не та форма")
        assertTrue(LINK in receipt.text, "ссылки на чек нет в словах: ${receipt.text}")
    }

    @Test
    fun `без ссылки мессенджер компьютера не открывается, и сказано почему`() {
        linked(null)
        val share = FakeShareOut(ways = listOf(ShareWay.WhatsApp, ShareWay.Telegram, ShareWay.Email))
        val model = printModel(services, printPorts(share = share))

        model.sharing.share(PrintSource.Journal("d-1"), null, ShareWay.WhatsApp)

        assertTrue(share.shared.isEmpty(), "в мессенджер ушло сообщение без ссылки")
        assertEquals(texts.noLink, (notices.last as? Message.Refusal)?.text)
    }

    private companion object {
        const val LINK = "https://receipt.ecc.kz/check/000000000001"
        val PDF = "%PDF-1.7".encodeToByteArray()
    }
}
