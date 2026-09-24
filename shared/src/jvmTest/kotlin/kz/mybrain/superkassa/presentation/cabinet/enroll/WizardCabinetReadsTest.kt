package kz.mybrain.superkassa.presentation.cabinet.enroll

import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.domain.cabinet.model.KkmModel
import kz.mybrain.superkassa.domain.cabinet.model.RegisterCreate
import kz.mybrain.superkassa.domain.cabinet.model.RetailPlace
import kz.mybrain.superkassa.domain.cabinet.port.CabinetPlaces
import kz.mybrain.superkassa.domain.cabinet.port.CabinetRegisters
import kz.mybrain.superkassa.domain.cabinet.unwired
import kz.mybrain.superkassa.presentation.cabinet.CabinetScene
import kz.mybrain.superkassa.presentation.cabinet.onTestClock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Путь мастера в кабинете без обхода сети.
 *
 * Живая проверка на кабинете с 3287 кассами: после «Создать кассу» мастер
 * перебирал страницами все кассы (двести запросов подряд) и все точки.
 * Мастеру нужна одна касса: вход не читает хозяйство сети, точка ищется
 * поиском кабинета одной страницей, а созданная касса известна по ответу
 * на её заведение.
 */
class WizardCabinetReadsTest {
    private val asked = mutableListOf<String>()
    private val place = RetailPlace(id = "p-1", name = "Магазин у дома")
    private val created = CabinetRegister(id = "r-9", kkmId = 5000021, status = "DRAFT")

    private val scene = CabinetScene().apply {
        ports.places = object : CabinetPlaces by unwired<CabinetPlaces>() {
            override suspend fun all(onPart: (List<RetailPlace>, Long) -> Unit): List<RetailPlace> =
                emptyList<RetailPlace>().also { asked += "places all" }

            override suspend fun search(text: String): List<RetailPlace> =
                listOf(place).also { asked += "places search" }
        }
        ports.registers = object : CabinetRegisters by unwired<CabinetRegisters>() {
            override suspend fun all(onPart: (List<CabinetRegister>, Long) -> Unit): List<CabinetRegister> =
                emptyList<CabinetRegister>().also { asked += "registers all" }

            override suspend fun blocked(): Set<String> = emptySet<String>().also { asked += "blocked" }

            override suspend fun models(): List<KkmModel> = listOf(KkmModel("0x01", "Суперкасса"))

            override suspend fun add(register: RegisterCreate): CabinetRegister = created.also { asked += "add" }
        }
    }

    @Test
    fun `вход из мастера, поиск точки и заведение кассы обходятся без чтения всей сети`() = onTestClock {
        scene.cabinet.signIn(lists = false)
        val form = AddRegisterViewModel(scene.cabinet)
        form.open()
        val draft = RegisterDraft(FactoryStamp("SK00100001", "2026")).apply {
            this.place = form.state.value.places.single()
            model = form.state.value.models.single()
        }
        var added: CabinetRegister? = null

        form.add(draft, null) { added = it }

        assertEquals(created, added)
        assertEquals(listOf("places search", "add"), asked, "мастер обходил сеть")
        assertTrue(created in scene.cabinet.state.value.registers, "созданной кассы нет в списке окна")
    }

    @Test
    fun `раздел кабинета, открытый после мастера, читает хозяйство сам`() = onTestClock {
        scene.cabinet.signIn(lists = false)
        assertTrue(asked.isEmpty(), "вход из мастера читал хозяйство: $asked")

        scene.cabinet.reload()

        assertTrue("places all" in asked && "registers all" in asked, "раздел не прочёл хозяйство: $asked")
    }
}
