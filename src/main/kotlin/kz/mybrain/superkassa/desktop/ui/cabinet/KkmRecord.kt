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
 * Смыслов пять, и идут они жизнью кассы: [Entered] — заведена
 * в кабинете, заявление в КГД не подавалось; [Applied] — заявление
 * подано и ждёт ответа; [OnRecord] — касса работает по закону;
 * [Refused] — вмешаться; [Deregistered] — дело закончено, беспокоиться
 * не о чем.
 *
 * Заведённая и поданная разведены не ради подробности. Прежде они
 * считались вместе и назывались «учёт идёт», а в сети показа из 3294
 * касс 3288 заведены и ни одного заявления не подано: экран обещал
 * три тысячи заявлений в КГД, которых нет.
 */
enum class KkmRecord { Entered, Applied, OnRecord, Refused, Deregistered }

/**
 * Смысл кода состояния, пришедшего от кабинета.
 *
 * Незнакомый код — [KkmRecord.Refused]: молча считать благополучным то,
 * чего приложение не понимает, нельзя. Отсутствие кода — другое дело:
 * кабинет о состоянии не сказал вовсе, и поднимать из-за этого тревогу
 * не за что. Такая касса считается заведённой — это самое малое, что
 * о ней известно наверняка, и заявления оно ей не приписывает.
 */
fun kkmRecord(status: String?): KkmRecord = when (status?.trim()?.uppercase().orEmpty()) {
    "REGISTERED", "REGISTERED_REREGISTRATION_SUCCESS" -> KkmRecord.OnRecord
    "REGISTRATION_IN_ISNA_PROCESS",
    "REREGISTRATION_IN_ISNA_PROCESS",
    "DEREGISTRATION_IN_ISNA_PROCESS" -> KkmRecord.Applied
    "DRAFT", "" -> KkmRecord.Entered
    "DEREGISTERED" -> KkmRecord.Deregistered
    else -> KkmRecord.Refused
}

/** Стоит ли касса на учёте: самый частый вопрос к состоянию. */
fun onRecord(status: String?): Boolean = kkmRecord(status) == KkmRecord.OnRecord
