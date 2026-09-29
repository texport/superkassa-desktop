package kz.mybrain.superkassa.domain.cabinet.model.documents

/**
 * Где документ кабинета по дороге в КГД — одно состояние на документ.
 *
 * Кабинет отдаёт два поля о двух плечах передачи: `deliveryStatus` —
 * итог в КГД (`SENT`, `DELIVERED`, `REJECTED`, `FAILED`), `sendStatus` —
 * передача службе, которая везёт документ в КГД (`ACCEPTED`,
 * `IN_PROGRESS`, `FAILED`). Итог в КГД важнее передачи: есть итог —
 * о передаче говорить нечего. Прежде у каждого документа стояло «Принят»:
 * передача службе принималась за приём в КГД.
 *
 * В КГД передаются только чеки — любого вида — и Z-отчёт. X-отчёт и движение
 * денег не передаются никогда: их состояние одно, что бы ни пришло в полях
 * кабинета, и о доставке оно ничего не обещает.
 */
enum class KgdDelivery(val tone: KgdTone) {
    /** Принят КГД. */
    Accepted(KgdTone.Done),

    /** Отклонён КГД — причина в карточке документа. */
    Rejected(KgdTone.Refused),

    /** Не доставлен в КГД: попытки исчерпаны. */
    Failed(KgdTone.Refused),

    /** Отправлен в КГД, ответа ещё нет. */
    Sent(KgdTone.Waiting),

    /** Передаётся службе передачи в КГД. */
    Transferring(KgdTone.Waiting),

    /** Ошибка передачи службе — будет повтор. */
    TransferFailed(KgdTone.Refused),

    /** Ждёт отправки в КГД. */
    Awaiting(KgdTone.Waiting),

    /** В КГД не передаётся: X-отчёт, внесение, изъятие. */
    NotSent(KgdTone.Neutral);

    /** Сопоставление кодов кабинета с состоянием — одно на приложение. */
    companion object {
        /** Чек любого вида: в КГД передаётся всегда. */
        fun ofReceipt(delivery: String?, send: String?): KgdDelivery = byDelivery(delivery) ?: bySend(send)

        /** Отчёт: в КГД передаётся только Z-отчёт; X-отчёт и прочие — нет. */
        fun ofReport(type: String?, delivery: String?, send: String?): KgdDelivery =
            if (type?.trim()?.uppercase() == Z_REPORT) ofReceipt(delivery, send) else NotSent

        /** Внесение и изъятие денег в КГД не передаются никогда. */
        fun ofMovement(): KgdDelivery = NotSent

        private fun byDelivery(code: String?): KgdDelivery? = when (code?.trim()?.uppercase()) {
            "DELIVERED" -> Accepted
            "REJECTED" -> Rejected
            "FAILED" -> Failed
            "SENT" -> Sent
            else -> null
        }

        private fun bySend(code: String?): KgdDelivery = when (code?.trim()?.uppercase()) {
            "IN_PROGRESS" -> Transferring
            "FAILED" -> TransferFailed
            else -> Awaiting
        }

        /** Вид отчёта, который уходит в КГД. */
        private const val Z_REPORT = "Z"
    }
}

/** Смысл состояния для цвета: сделано, ожидание, отказ или нейтрально. */
enum class KgdTone { Done, Waiting, Refused, Neutral }
