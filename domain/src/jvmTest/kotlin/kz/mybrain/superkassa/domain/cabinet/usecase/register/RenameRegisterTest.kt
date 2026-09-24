package kz.mybrain.superkassa.domain.cabinet.usecase.register

import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.RegisterName
import kz.mybrain.superkassa.domain.cabinet.port.CabinetRegisters
import kz.mybrain.superkassa.domain.cabinet.unwired
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Своё название кассы у владельца.
 *
 * Кабинет требует название всегда — не пустое и не длиннее ста двадцати
 * знаков — и стереть его не даёт: пустое он отвергал проверкой полей,
 * и владелец читал отказ о том, что приложение само ему позволило.
 */
class RenameRegisterTest {
    private val sent = mutableListOf<String>()

    private val registers = object : CabinetRegisters by unwired<CabinetRegisters>() {
        override suspend fun rename(id: String, name: String): CabinetRegister =
            CabinetRegister(id = id, kkmId = 1, status = "DRAFT", internalName = name).also { sent += name }
    }

    private val rename = RenameRegister(registers)

    @Test
    fun `название уходит без пробелов по краям`() {
        val renamed = runBlocking { rename("r-1", "  Касса у входа ") }

        assertEquals(listOf("Касса у входа"), sent)
        assertEquals("Касса у входа", renamed?.internalName)
    }

    @Test
    fun `пустое название к кабинету не уходит`() {
        assertNull(runBlocking { rename("r-1", "   ") })
        assertTrue(sent.isEmpty(), "пустое название ушло кабинету, а он его отвергает")
    }

    @Test
    fun `название длиннее предела кабинета не уходит`() {
        val long = "К".repeat(RegisterName.MAX_LENGTH + 1)

        assertNull(runBlocking { rename("r-1", long) })
        assertEquals("К".repeat(RegisterName.MAX_LENGTH), RegisterName.of("К".repeat(RegisterName.MAX_LENGTH)))
    }
}
