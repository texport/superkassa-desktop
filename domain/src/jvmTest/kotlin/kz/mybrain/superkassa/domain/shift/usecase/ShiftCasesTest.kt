package kz.mybrain.superkassa.domain.shift.usecase

import io.github.texport.superkassa.core.presentation.api.model.ofd.OfdCommandResponse
import io.github.texport.superkassa.core.presentation.api.model.ofd.OfdCommandStatus
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.shift.model.ShiftPart
import kz.mybrain.superkassa.domain.shift.model.ShiftState
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.DashboardScene
import kz.mybrain.superkassa.kassa.FakeCore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Сценарии главного экрана на тестовом ядре, без экрана: касса и пин — у держателя входа. */
class ShiftCasesTest {
    private val signIn = SignIn().apply { enter(CoreScene.kkm(), CoreScene.cashier(), CoreScene.PIN) }

    @Test
    fun `смена со слов кассы открыта, хотя документы не отданы`(): Unit = runBlocking {
        val core = DashboardScene.core().apply { refuse("listShiftDocuments", "KKM_BLOCKED") }

        val snapshot = assertNotNull(ReadShift(core.kassa(), signIn)())

        assertEquals(ShiftState.Open, snapshot.shift)
        assertEquals(null, snapshot.documents)
        assertEquals(ShiftPart.Documents, snapshot.trouble?.part)
    }

    @Test
    fun `смена, закрытая между обращениями, — закрыта, а не беда`(): Unit = runBlocking {
        val core = DashboardScene.core().apply { refuse("listShiftDocuments", "SHIFT_NOT_OPEN") }

        val snapshot = assertNotNull(ReadShift(core.kassa(), signIn)())

        assertEquals(ShiftState.Closed, snapshot.shift)
        assertEquals(null, snapshot.trouble)
    }

    @Test
    fun `связь с БФД — да или нет по ответу кассы`(): Unit = runBlocking {
        val core = FakeCore()
        core.on("checkOfdConnection") { OfdCommandResponse(status = OfdCommandStatus.OK) }
        assertEquals(Answer.Done(true), CheckOfdLink(core.kassa(), signIn)())

        core.on("checkOfdConnection") { OfdCommandResponse(status = OfdCommandStatus.TIMEOUT) }
        assertEquals(Answer.Done(false), CheckOfdLink(core.kassa(), signIn)())

        core.refuse("checkOfdConnection", "KKM_BLOCKED")
        val refused = CheckOfdLink(core.kassa(), signIn)()
        assertEquals("KKM_BLOCKED", (refused as Answer.Refused).code)
    }

    /** Без входа команды не уходят: касса и пин вызова берутся только у вошедшего. */
    @Test
    fun `без кассира смена не читается`(): Unit = runBlocking {
        assertEquals(null, ReadShift(DashboardScene.core().kassa(), SignIn())())
    }
}
