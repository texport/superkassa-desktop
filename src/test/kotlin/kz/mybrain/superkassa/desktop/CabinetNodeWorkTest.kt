package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.TechnicalState
import kz.mybrain.superkassa.desktop.ui.cabinet.AdoptForm
import kz.mybrain.superkassa.desktop.ui.cabinet.AdoptLabels
import kz.mybrain.superkassa.desktop.ui.cabinet.NodeWork
import kz.mybrain.superkassa.desktop.ui.cabinet.adoptMissing
import kz.mybrain.superkassa.desktop.ui.cabinet.heardElsewhere
import kz.mybrain.superkassa.desktop.ui.cabinet.nodeWork
import kz.mybrain.superkassa.desktop.ui.components.OfdTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Работает ли касса кабинета на этой машине и когда её сюда заводят.
 *
 * Три состояния строки в паспорте кассы и одно действие в ней: ошибка
 * в этих правилах либо прячет действие у кассы, которой оно нужно, либо
 * предлагает завести кассу, которую КГД на учёт не ставил.
 */
class CabinetNodeWorkTest {

    private fun register(
        kkmId: Int = 42,
        status: String = "REGISTERED",
        number: String? = "KGD-2000302"
    ) = CabinetRegister(id = "id", kkmId = kkmId, status = status, registrationNumber = number)

    private fun kkm(systemId: String?) = Kkm(kkmId = "node-1", ofdSystemId = systemId)

    @Test
    fun `касса узнаётся по идентификатору у ОФД`() {
        val work = nodeWork(register(kkmId = 42), listOf(kkm("7"), kkm("42")))
        assertEquals(NodeWork.Here(kkm("42")), work)
    }

    @Test
    fun `заведённая здесь остаётся заведённой, что бы ни было с учётом`() {
        val work = nodeWork(register(status = "DEREGISTERED", number = null), listOf(kkm("42")))
        assertTrue(work is NodeWork.Here, "касса на узле есть — про неё нельзя сказать иначе")
    }

    @Test
    fun `без регистрационного номера работать нельзя`() {
        assertEquals(NodeWork.NotOnRecord, nodeWork(register(number = null), emptyList()))
    }

    @Test
    fun `черновик и снятая с учёта работать не дают`() {
        listOf("DRAFT", "DEREGISTERED", "REGISTRATION_IN_ISNA_PROCESS").forEach { status ->
            assertEquals(NodeWork.NotOnRecord, nodeWork(register(status = status), emptyList()), status)
        }
    }

    @Test
    fun `на учёте и не заведена — единственное состояние с действием`() {
        assertEquals(NodeWork.Absent, nodeWork(register(), listOf(kkm("7"))))
        assertEquals(NodeWork.Absent, nodeWork(register(status = "REGISTERED_REREGISTRATION_SUCCESS"), emptyList()))
    }

    @Test
    fun `чужой идентификатор не считается своим`() {
        assertEquals(NodeWork.Absent, nodeWork(register(kkmId = 42), listOf(kkm("420"), kkm(null))))
    }

    @Test
    fun `усиленное подтверждение нужно, когда ОФД кассу слышал`() {
        assertTrue(heardElsewhere(TechnicalState(found = true, lastContactAt = "2026-09-10T08:15:00Z")))
        assertTrue(heardElsewhere(TechnicalState(found = true, shiftStatus = "OPEN")))
    }

    @Test
    fun `молчащая касса отдельной отметки не требует`() {
        assertTrue(!heardElsewhere(null))
        assertTrue(!heardElsewhere(TechnicalState(found = true, shiftStatus = "CLOSED")))
        assertTrue(!heardElsewhere(TechnicalState(found = true, lastContactAt = "  ")))
    }

    @Test
    fun `без пина действие недоступно`() {
        val missing = adoptMissing(form(adminPin = "12"), labels)
        assertEquals(listOf(labels.adminPin), missing)
    }

    @Test
    fun `стандартный пин узел не примет — действие недоступно`() {
        assertEquals(listOf(labels.adminPin), adoptMissing(form(adminPin = "0000"), labels))
    }

    @Test
    fun `без заданного ОФД действие недоступно`() {
        assertEquals(listOf(labels.ofd), adoptMissing(form(ofdComplete = false), labels))
    }

    /**
     * Адрес БФД знает узел: владельцу достаточно выбрать контур.
     *
     * Прежде проверка требовала ещё и выбранного поставщика. Поставщик
     * теперь подставлен — он один на продукт, — и требование к выбору
     * заперло бы окно на поле, которого на экране нет.
     */
    @Test
    fun `выбор полон с подставленным поставщиком и выбранным контуром`() {
        assertTrue(OfdTarget(environment = "DEV").complete)
        assertTrue(!OfdTarget().complete)
        assertTrue(!OfdTarget(provider = "", environment = "DEV").complete)
    }

    @Test
    fun `слышанная ОФД касса требует отдельной отметки`() {
        assertEquals(listOf(labels.handover), adoptMissing(form(handoverNeeded = true), labels))
        assertEquals(
            emptyList(),
            adoptMissing(form(handoverNeeded = true, handoverAccepted = true), labels)
        )
    }

    @Test
    fun `заполненное окно открывает действие`() {
        assertEquals(emptyList(), adoptMissing(form(), labels))
    }

    /** Подписи полей: в правилах они не объявляются, а приходят из надписей. */
    private val labels = AdoptLabels(
        ofd = "ОФД",
        adminPin = "Пин администратора",
        handover = "Отметка о последствии"
    )

    /** Заполненное окно; каждый случай портит ровно одно поле. */
    private fun form(
        ofdComplete: Boolean = true,
        adminPin: String = "4821",
        handoverNeeded: Boolean = false,
        handoverAccepted: Boolean = false
    ) = AdoptForm(ofdComplete, adminPin, handoverNeeded, handoverAccepted)
}
