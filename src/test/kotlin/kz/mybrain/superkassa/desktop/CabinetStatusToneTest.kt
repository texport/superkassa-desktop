package kz.mybrain.superkassa.desktop

import kz.mybrain.superkassa.desktop.ui.cabinet.statusTone
import kz.mybrain.superkassa.desktop.ui.components.StatusTone
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Цвет состояния кассы отличает этап от беды.
 *
 * Ролей четыре, и в кабинете они значат одно и то же: зелёная — сделано,
 * жёлтая — ждём чужого ответа, красная — вмешаться, серая — обычное
 * состояние покоя. Жёлтым красился и черновик, который никто никуда
 * не подавал: у сети показа таких три тысячи из трёх тысяч трёхсот,
 * и колонка стояла жёлтой сверху донизу — рядом с тремя кассами,
 * по которым КГД и правда думает. Ожидание, которого нет ни у кого,
 * это не ожидание.
 */
class CabinetStatusToneTest {

    @Test
    fun `черновик — покой, а не ожидание ответа`() {
        assertEquals(StatusTone.Idle, statusTone("DRAFT"))
    }

    @Test
    fun `ждём ответа КГД — ожидание`() {
        listOf(
            "REGISTRATION_IN_ISNA_PROCESS",
            "REREGISTRATION_IN_ISNA_PROCESS",
            "DEREGISTRATION_IN_ISNA_PROCESS"
        ).forEach { assertEquals(StatusTone.Waiting, statusTone(it), it) }
    }

    @Test
    fun `учтённая — сделано, отказ КГД — вмешаться, снятая — покой`() {
        assertEquals(StatusTone.Good, statusTone("REGISTERED"))
        assertEquals(StatusTone.Good, statusTone("REGISTERED_REREGISTRATION_SUCCESS"))
        assertEquals(StatusTone.Bad, statusTone("REGISTRATION_IN_ISNA_ERROR"))
        assertEquals(StatusTone.Bad, statusTone("REGISTERED_REREGISTRATION_ERROR"))
        assertEquals(StatusTone.Idle, statusTone("DEREGISTERED"))
    }
}
