package kz.mybrain.superkassa.desktop.ui.cabinet

/**
 * Что КГД знает о кассе.
 *
 * Кабинет различает одиннадцать состояний учёта, а приложение спрашивало
 * у них одно: стоит касса на учёте или нет. В эту «нет» попадали и касса,
 * которой КГД отказал, и черновик, который владелец завёл час назад
 * и ещё не подал. Разница между ними — вся разница между «бежать чинить»
 * и «идти дальше по делу».
 *
 * Смыслов четыре, и каждый отвечает на свой вопрос владельца:
 * [OnRecord] — касса работает по закону; [InProgress] — заявление
 * в работе или ещё пишется, ждать; [Refused] — вмешаться;
 * [Deregistered] — дело закончено, беспокоиться не о чем.
 */
enum class KkmRecord { OnRecord, InProgress, Refused, Deregistered }

/**
 * Смысл кода состояния, пришедшего от кабинета.
 *
 * Незнакомый код — [KkmRecord.Refused]: молча считать благополучным то,
 * чего приложение не понимает, нельзя. Отсутствие кода — другое дело:
 * кабинет о состоянии не сказал вовсе, и поднимать из-за этого тревогу
 * не за что.
 */
fun kkmRecord(status: String?): KkmRecord = when (status?.trim()?.uppercase().orEmpty()) {
    "REGISTERED", "REGISTERED_REREGISTRATION_SUCCESS" -> KkmRecord.OnRecord
    "DRAFT",
    "REGISTRATION_IN_ISNA_PROCESS",
    "REREGISTRATION_IN_ISNA_PROCESS",
    "DEREGISTRATION_IN_ISNA_PROCESS" -> KkmRecord.InProgress
    "DEREGISTERED" -> KkmRecord.Deregistered
    "" -> KkmRecord.InProgress
    else -> KkmRecord.Refused
}

/** Стоит ли касса на учёте: самый частый вопрос к состоянию. */
fun onRecord(status: String?): Boolean = kkmRecord(status) == KkmRecord.OnRecord
