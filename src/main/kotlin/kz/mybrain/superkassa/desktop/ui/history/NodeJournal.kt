package kz.mybrain.superkassa.desktop.ui.history

import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Document
import kz.mybrain.superkassa.desktop.server.PAGE
import kz.mybrain.superkassa.desktop.server.documents
import kz.mybrain.superkassa.desktop.ui.components.INTERNAL
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.components.SENT
import kz.mybrain.superkassa.desktop.ui.components.SHIFT_OPEN
import kz.mybrain.superkassa.desktop.ui.strings.AppStrings
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import java.time.LocalDate

/**
 * Документы узла строками общего журнала.
 *
 * Узел — первый из двух источников журнала; второй, кабинет, приводит
 * свои записи к тем же строкам. Дальше показ, поиск и отбор одинаковы,
 * и то, что кассир видит у кассы, владелец находит в кабинете.
 */
fun journalEntriesOf(session: Session, texts: AppStrings, documents: List<Document>): List<JournalEntry> =
    documents.map { document ->
        JournalEntry(
            key = document.id,
            at = document.createdAt,
            moment = momentText(document.createdAt),
            typeCode = document.docType,
            type = documentTypeTitle(session, texts, document.docType),
            number = document.docNo?.toString() ?: Glyphs.DASH,
            numberOrder = document.docNo,
            amount = Money.formatTiyn(document.totalAmount),
            amountOrder = document.totalAmount?.let { Money.tengeOf(it) },
            // Автономный признак показан наравне с фискальным: у документа,
            // пробитого без связи, фискального признака ещё нет, и пустая
            // клетка выглядела бы утратой документа.
            sign = document.fiscalSign ?: document.autonomousSign ?: Glyphs.DASH,
            delivery = deliveryOf(document),
            shiftNo = document.shiftNo?.toLong(),
            printable = document.printable
        )
    }

/**
 * Состояние доставки документа узла.
 *
 * Незнакомый код состоянием не становится: протокольных кодов на экране
 * кассира быть не должно, а угадывать смысл кода — значит однажды
 * покрасить отказ зелёным.
 */
private fun deliveryOf(document: Document): JournalDelivery? = when {
    // Открытие смены в ОФД не уходит никогда: команды COMMAND_OPEN_SHIFT
    // в протоколе нет. Прежние записи хранят у него состояние доставки,
    // но кассиру оно всё равно ничего не обещает.
    document.docType == SHIFT_OPEN -> JournalDelivery.Internal
    document.ofdStatus == INTERNAL -> JournalDelivery.Internal
    document.ofdStatus == SENT && document.isAutonomous == true -> JournalDelivery.Resent
    document.ofdStatus == SENT -> JournalDelivery.Delivered
    document.ofdStatus == REFUSED -> JournalDelivery.Refused
    document.ofdStatus == QUEUED -> JournalDelivery.Queued
    else -> null
}

/**
 * Дочитывает срок с того места, где остановились.
 *
 * Страницами: за месяц оживлённой кассы документов десятки тысяч, и одним
 * запросом узел их не отдаёт. Срок без границ читается с начала записей
 * узла и до конца сегодняшнего дня.
 *
 * @return есть ли за пришедшей страницей ещё документы.
 */
internal suspend fun loadPeriod(
    session: Session,
    what: String,
    period: JournalPeriod,
    into: MutableList<Document>
): Boolean {
    val kkm = session.selected ?: return false
    val from = period.range?.fromMillis() ?: FIRST_RECORD
    val to = period.range?.toMillis() ?: dayRange(LocalDate.now()).toMillis
    val loaded = session.guard(what) {
        session.client.documents(kkm.kkmId, from, to, session.pin, into.size)
    } ?: return false
    into.addAll(loaded)
    return loaded.size == PAGE
}

/** Ответ ОФД: документ отвергнут. */
private const val REFUSED = "FAILED"

/** Документ ждёт отправки в ОФД. */
private const val QUEUED = "PENDING"

/** С какого момента читается срок без границ: с начала счёта времени. */
private const val FIRST_RECORD = 0L
