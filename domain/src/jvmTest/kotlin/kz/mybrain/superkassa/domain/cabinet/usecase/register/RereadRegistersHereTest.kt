package kz.mybrain.superkassa.domain.cabinet.usecase.register

import kotlinx.coroutines.runBlocking
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.port.CabinetRegisters
import kz.mybrain.superkassa.domain.cabinet.unwired
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.FakeCore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Касса, поставленная на учёт мастером, в прочитанном раньше списке
 * кабинета оставалась черновиком. Перечитываются только кассы этой машины:
 * весь список сети — сотни обращений.
 */
class RereadRegistersHereTest {
    private val core = FakeCore()
    private val asked = mutableListOf<String>()
    private val registers = object : CabinetRegisters by unwired() {
        override suspend fun one(id: String): CabinetRegister {
            asked += id
            if (id == BROKEN.id) error("кабинет не ответил")
            return HERE.copy(id = id, status = "REGISTERED")
        }
    }
    private val reread = RereadRegistersHere(core.kassa(), registers)

    @Test
    fun `перечитывается только касса этой машины`(): Unit = runBlocking {
        core.on("listKkms") { CoreScene.page(listOf(CoreScene.kkm().copy(ofdSystemId = HERE.kkmId.toString()))) }

        val fresh = reread(listOf(HERE, ELSEWHERE))

        assertEquals(listOf(HERE.id), asked, "перечитывались кассы чужих машин")
        assertEquals("REGISTERED", fresh.single().status)
    }

    @Test
    fun `касса, которую кабинет не отдал, остаётся прежней`(): Unit = runBlocking {
        core.on("listKkms") { CoreScene.page(listOf(CoreScene.kkm().copy(ofdSystemId = BROKEN.kkmId.toString()))) }

        assertTrue(reread(listOf(BROKEN)).isEmpty())
    }

    @Test
    fun `касса машины не ответила — кабинет не спрашивается`(): Unit = runBlocking {
        core.refuse("listKkms", "DB_LOCKED", ru = "База занята")

        assertTrue(reread(listOf(HERE)).isEmpty())
        assertTrue(asked.isEmpty())
    }

    private companion object {
        val HERE = CabinetRegister(
            id = "r-1",
            kkmId = 5_005_026,
            internalName = "Касса у входа",
            status = "DRAFT",
            registrationNumber = null,
            factoryNumber = "SN-ECC-172758",
            manufactureYear = 2026
        )
        val ELSEWHERE = HERE.copy(id = "r-2", kkmId = 5_005_027)
        val BROKEN = HERE.copy(id = "r-3", kkmId = 5_005_028)
    }
}
