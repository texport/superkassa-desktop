package kz.mybrain.superkassa.presentation.shift.dashboard

import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import io.github.texport.superkassa.core.presentation.api.model.shift.ReportResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.DashboardScene
import kz.mybrain.superkassa.kassa.services
import kz.mybrain.superkassa.presentation.common.message.Message
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Итог X-отчёта, который БФД не принял: отказ с причиной и кодом.
 *
 * Владелец снимал X-отчёт за X-отчётом: касса говорила «сформирован»
 * и «отклонён» без причины, а в журнале стояло «x report: done».
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardReportRefusalTest {
    private val signIn = SignIn()
    private val notices = Notices()
    private val texts = textsOf(Language.Ru).common

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    /**
     * Отказ БФД — отказ с причиной и кодом, а не зелёное «X-отчёт сформирован».
     *
     * Владелец снимал X-отчёт за X-отчётом: касса говорила «сформирован»
     * и «отклонён» без причины, а в журнале стояло «x report: done».
     */
    @Test
    fun `X-отчёт, не принятый БФД, объявляется отказом с причиной и кодом`() {
        val why = TrilingualMessageResponse("Неверный токен", "Токен қате", "Wrong token")
        val core = DashboardScene.core().apply {
            on("createReport") {
                ReportResponse(
                    documentId = "x-2",
                    deliveryStatus = DeliveryStatus.ONLINE_ERROR,
                    deliveryError = why,
                    bfdResultCode = 3
                )
            }
        }
        val model = dashboardModel(CoreScene.services(core, signIn, notices))
        signIn.enter(CoreScene.kkm(), CoreScene.cashier(true), CoreScene.PIN)

        model.xReport()

        val said = notices.last as? Message.Refusal
        assertEquals("${texts.dashboard.xReportDone}, но БФД его не принял: Неверный токен", said?.text)
        assertEquals("3", said?.code)
    }
}
