package kz.mybrain.superkassa.desktop.tools

import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.FiscalResult
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.listKkms

/**
 * Отчёт прогона.
 *
 * Очередь считается успехом наравне с доставкой: чек пробит, фискальный
 * эффект применён, и досылка догонит. Отказом считается только отказ.
 */
class CycleReport {
    private val lines = mutableListOf<String>()
    private var failures = 0

    fun done(what: String, detail: String) {
        lines += "  ок     $what — $detail"
    }

    fun fail(what: String, detail: String) {
        lines += "  ОТКАЗ  $what — $detail"
        failures++
    }

    /** Выполняет фискальную команду и записывает исход её же словами. */
    suspend fun step(session: Session, what: String, action: suspend () -> FiscalResult) {
        val result = session.guard(what) { action() }
        if (result == null) {
            fail(what, session.lastMessage.toString())
            return
        }
        when {
            result.isDelivered -> done(what, "доставлено в ОФД")
            result.isQueued -> done(what, "поставлено в очередь: связи нет")
            else -> fail(what, "состояние доставки ${result.deliveryStatus}")
        }
    }

    /** Свод по журналу смены: что и в каком состоянии осталось. */
    fun summary(documents: List<Document>) {
        lines += ""
        lines += "  Документов в смене: ${documents.size}"
        documents.groupBy { it.docType ?: "—" }.forEach { (type, group) ->
            val delivered = group.count { it.ofdStatus == "SENT" }
            lines += "    $type: ${group.size}, доставлено $delivered"
        }
        val undelivered = documents.count { it.ofdStatus != "SENT" }
        if (undelivered > 0) {
            lines += "  Не доставлено: $undelivered"
        }
        val duplicates = documents.size - documents.map { it.id }.toSet().size
        if (duplicates > 0) {
            fail("Задвоение документов", "повторов $duplicates")
        }
    }

    fun print() {
        println("=== Полный цикл кассы ===")
        lines.forEach(::println)
        println(if (failures == 0) "Пройдено полностью" else "Неудач: $failures")
    }
}

/** Находит уже заведённую кассу по номеру в ОФД. */
suspend fun Session.findExisting(systemId: String): Kkm? {
    val list = guard("Список касс") { client.listKkms() } ?: return null
    return list.firstOrNull { it.ofdSystemId == systemId }
}
