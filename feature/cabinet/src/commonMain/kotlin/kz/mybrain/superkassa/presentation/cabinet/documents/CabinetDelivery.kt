package kz.mybrain.superkassa.presentation.cabinet.documents

import kz.mybrain.superkassa.domain.cabinet.model.documents.KgdDelivery
import kz.mybrain.superkassa.domain.cabinet.model.documents.KgdTone
import kz.mybrain.superkassa.presentation.common.document.JournalDelivery
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts
import kotlin.time.Instant

/**
 * Есть ли у документа кабинета печатная форма.
 *
 * Документ, который КГД отклонил или так и не получил, стоит в отчётности
 * под вопросом, а печатная форма выглядит как настоящий чек — с номером,
 * признаком и QR-кодом, — и покупатель принимает её за подтверждение
 * покупки. Остальные печатаются: ошибка передачи службе — временная,
 * её повторят, а X-отчёт и движение денег в КГД не уходят вовсе.
 */
internal fun KgdDelivery.drawable(): Boolean = this != KgdDelivery.Rejected && this != KgdDelivery.Failed

/**
 * Состояние в КГД — группой общего журнала: по ней цвет плашки, отбор
 * по состоянию и печать. Слова у кабинета свои ([words]): три слова
 * журнала кассы не различают «передаётся» и «отправлен в КГД».
 */
internal fun KgdDelivery.journal(): JournalDelivery = when (tone) {
    KgdTone.Done -> JournalDelivery.Delivered
    KgdTone.Waiting -> JournalDelivery.Queued
    KgdTone.Refused -> JournalDelivery.Refused
    KgdTone.Neutral -> JournalDelivery.Internal
}

/** Слова состояния в КГД — одни на строку журнала и карточку документа. */
internal fun KgdDelivery.words(texts: CabinetTexts): String = when (this) {
    KgdDelivery.Accepted -> texts.documents.kgdAccepted
    KgdDelivery.Rejected -> texts.documents.kgdRejected
    KgdDelivery.Failed -> texts.documents.kgdFailed
    KgdDelivery.Sent -> texts.documents.kgdSent
    KgdDelivery.Transferring -> texts.documents.kgdTransferring
    KgdDelivery.TransferFailed -> texts.documents.kgdTransferFailed
    KgdDelivery.Awaiting -> texts.documents.kgdAwaiting
    KgdDelivery.NotSent -> texts.documents.kgdNotSent
}

/**
 * Момент документа числом.
 *
 * Кабинет отдаёт время строкой ISO-8601 в UTC; журнал сортирует строки
 * по числу — по тексту «2026-09-07T19:42» и «2026-10-01T08:00» сравнились
 * бы верно, а «07.09.2026» и «01.10.2026» из показа — уже нет.
 */
internal fun cabinetMillis(iso: String?): Long? {
    val value = iso?.takeIf { it.isNotBlank() } ?: return null
    return runCatching { Instant.parse(value).toEpochMilliseconds() }.getOrNull()
}
