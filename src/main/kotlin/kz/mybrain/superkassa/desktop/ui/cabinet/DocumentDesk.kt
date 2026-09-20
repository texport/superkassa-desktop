package kz.mybrain.superkassa.desktop.ui.cabinet

import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.PrintFileName
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.history.JournalEntry
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts

/**
 * Просмотр и печать документа кабинета.
 *
 * Своего вида документа здесь нет и быть не должно: вид документа один,
 * и рисует его рисовальщик кассы. Прежде это значило, что строка кабинета
 * сводилась к документу в журнале узла, а документ, пробитый на другой
 * машине, не открывался вовсе — владельцу отвечали, что его форму рисует
 * та касса.
 *
 * Теперь на узел уходит сам документ: кабинет отдаёт его пакетом
 * протокола — запросом кассы и ответом ОФД, — и узел рисует форму
 * по переданным данным. Пробитое на чужой машине открывается так же,
 * как своё.
 */
class DocumentDesk(
    private val session: Session,
    private val cabinet: CabinetSession,
    private val texts: CabinetTexts
) {

    /** Показывает печатную форму документа поверх списка. */
    suspend fun preview(registerId: String, kind: DocumentKind, entry: JournalEntry, target: RowTarget?) {
        val packet = packetOf(registerId, kind, target) ?: return
        session.printDesk.previewPacket(
            packet = packet,
            name = entry.key,
            file = PrintFileName.of(entry.typeCode, entry.number, entry.shiftNo)
        )
    }

    /** Печатает документ, не открывая его. */
    suspend fun print(registerId: String, kind: DocumentKind, target: RowTarget?) {
        val packet = packetOf(registerId, kind, target) ?: return
        session.printDesk.printPacket(packet)
    }

    /**
     * Пакет протокола документа строки.
     *
     * Документ берётся тем же обращением, которым он раскрывается на
     * экране: список отдаёт только сводку строки, а пакет лежит в самом
     * документе. Отказ кабинета уже показан общей строкой помехи —
     * второго сообщения о том же не нужно.
     */
    private suspend fun packetOf(registerId: String, kind: DocumentKind, target: RowTarget?): String? {
        val packet = when (val opened = openDocument(cabinet, registerId, kind, target)) {
            is OpenedDocument.Receipt -> opened.details.packet
            is OpenedDocument.Report -> opened.details.packet
            is OpenedDocument.Movement -> opened.details.packet
            else -> null
        }
        if (packet == null && cabinet.problem == null) {
            session.report(texts.documentDataMissing)
        }
        return packet
    }
}
