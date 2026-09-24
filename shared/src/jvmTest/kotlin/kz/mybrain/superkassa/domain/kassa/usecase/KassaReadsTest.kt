package kz.mybrain.superkassa.domain.kassa.usecase

import io.github.texport.superkassa.core.presentation.api.model.kkm.CounterSnapshotResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.DocumentDetailsResponse
import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kz.mybrain.superkassa.kassa.MemoryWorkplace
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Чтения продажи, возврата и денег — на тестовом ядре, без экрана.
 *
 * Каждое чтение — своим обращением: беда одного не отменяет прочих,
 * и первая беда названа, чтобы экран сказал о ней кассиру.
 */
class KassaReadsTest {

    /** За кассой сел кассир: сценарии берут кассу и пин у входа. */
    private val signed = SignIn().apply { enter(CoreScene.kkm(), CoreScene.cashier(), CoreScene.PIN) }

    private fun core(documents: List<String> = emptyList()) = FakeCore().apply {
        on("getLocalOpenShift") { CoreScene.openShift() }
        on(
            "listCounters"
        ) { listOf(CounterSnapshotResponse(scope = "GLOBAL", key = "cash.sum", value = 9_000, updatedAt = 0)) }
        on("listFiscalDocumentsByPeriod") { args ->
            if (args[4] == 0) {
                documents.mapIndexed { at, type ->
                    CoreScene.document(
                        "d-$at",
                        type = type
                    )
                }
            } else {
                emptyList()
            }
        }
    }

    @Test
    fun `ящик — только внесения и изъятия, последние десять`(): Unit = runBlocking {
        val day = List(12) { "CASH_IN" } + listOf("SALE", "CASH_OUT")

        val drawer = checkNotNull(ReadDrawer(core(day).kassa(), signed)(NOW))

        assertTrue(drawer.shiftOpen == true)
        assertEquals(9_000L, drawer.cash)
        assertEquals(10, drawer.recent?.size)
        assertTrue(drawer.recent.orEmpty().all { it.docType == "CASH_IN" || it.docType == "CASH_OUT" })
        assertNull(drawer.trouble)
    }

    @Test
    fun `молчание кассы о сутках — беда, а не пустые сутки`(): Unit = runBlocking {
        val core = core().apply { on("listFiscalDocumentsByPeriod") { error("disk") } }

        val drawer = checkNotNull(ReadDrawer(core.kassa(), signed)(NOW))

        assertNull(drawer.recent)
        assertIs<Answer.Failed>(drawer.trouble)
        assertEquals(9_000L, drawer.cash, "беда списка отменила остаток")
    }

    @Test
    fun `день возврата несёт отрасль места и первую беду`(): Unit = runBlocking {
        val core = core(listOf("SALE")).apply { refuse("listCounters", "PIN_LOCKED", ru = "Пин заперт на 4 мин") }
        val memory = MemoryWorkplace(domains = mapOf("kkm-1" to "DOMAIN_TAXI"))

        val day = checkNotNull(ReadReturnDay(core.kassa(), signed, memory)(0, NOW))

        assertEquals("DOMAIN_TAXI", day.domainCode)
        assertEquals(1, day.documents?.size)
        assertNull(day.cash)
        assertEquals("PIN_LOCKED", (day.trouble as Answer.Refused).code)
    }

    @Test
    fun `смену, о которой касса молчит, продажа берёт из сведений о кассе`(): Unit = runBlocking {
        val core = FakeCore().apply { on("getLocalOpenShift") { error("disk") } }
        val kkm = CoreScene.kkm().copy(isShiftOpen = true)

        val seat = ReadSaleSeat(core.kassa(), signed, MemoryWorkplace())(kkm)

        assertTrue(seat.shiftOpen)
        assertNull(seat.domainCode)
    }

    @Test
    fun `закрытая смена названа закрытой`(): Unit = runBlocking {
        val core = FakeCore().apply { on("getLocalOpenShift") { null } }

        val seat = ReadSaleSeat(core.kassa(), signed, MemoryWorkplace())(CoreScene.kkm().copy(isShiftOpen = true))

        assertFalse(seat.shiftOpen)
    }

    @Test
    fun `строки основания — из подробностей документа, отказ — как есть`(): Unit = runBlocking {
        val core = FakeCore().apply {
            on("getDocumentDetails") { DocumentDetailsResponse(CoreScene.document("d-1"), emptyList()) }
        }
        assertEquals(Answer.Done(emptyList()), ReadBasisItems(core.kassa(), signed)("d-1"))

        core.refuse("getDocumentDetails", "DOCUMENT_NOT_FOUND")
        val refused = ReadBasisItems(core.kassa(), signed)("d-1")
        assertEquals("DOCUMENT_NOT_FOUND", (refused as Answer.Refused).code)
    }

    @Test
    fun `без кассира чтений нет — читать нечего и некому`(): Unit = runBlocking {
        val nobody = SignIn()

        assertNull(ReadDrawer(core().kassa(), nobody)(NOW))
        assertNull(ReadReturnDay(core().kassa(), nobody, MemoryWorkplace())(0, NOW))
        assertIs<Answer.Failed>(ReadBasisItems(core().kassa(), nobody)("d-1"))
    }

    private companion object {
        const val NOW = 1_789_000_000_000L
    }
}
