package kz.mybrain.superkassa.presentation.cabinet

import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.cabinet.CabinetStatusNames
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

/**
 * Название состояния словами.
 *
 * Код, которого в таблице нет, тоже называется словами, а не доходит
 * до экрана собой: у кассы-черновика кабинет отдаёт `UNKNOWN`, и в списке
 * касс места плашка смены так и стояла — «UNKNOWN».
 */
fun statusTitle(code: String, texts: CabinetTexts): String =
    statusWords(code, texts) ?: texts.statuses.unknown

/**
 * То же название, но `null` у состояния, которого таблица не знает.
 *
 * Нужно там, где о неизвестном лучше молчать: плашка «состояние
 * неизвестно» рядом с кассой не сообщает о ней ничего, а место в строке
 * списка занимает.
 */
fun statusWords(code: String, texts: CabinetTexts): String? {
    val upper = code.uppercase()
    val words = STATUS_WORDS.entries.firstOrNull { upper in it.key }?.value
    return words?.invoke(texts.statuses) ?: texts.statuses.inProcess.takeIf { upper.endsWith(IN_PROCESS) }
}

/**
 * Коды состояний и их названия.
 *
 * Три отказных состояния учёта не назывались никак и доходили до экрана
 * как «состояние неизвестно» — жёлтым, будто их ещё ждут.
 */
private val STATUS_WORDS: Map<Set<String>, (CabinetStatusNames) -> String> = mapOf(
    setOf("DRAFT") to { it.draft },
    setOf("REGISTERED", "REGISTERED_REREGISTRATION_SUCCESS") to { it.registered },
    setOf("DEREGISTERED") to { it.deregistered },
    setOf("REGISTRATION_IN_ISNA_ERROR", "REGISTERED_REREGISTRATION_ERROR", "DEREGISTRATION_ERROR") to { it.rejected },
    setOf("ACTIVE", "KKM_ACTIVE") to { it.active },
    setOf("INACTIVE", "KKM_INACTIVE") to { it.inactive },
    setOf("BLOCKED", "KKM_BLOCKED") to { it.blocked },
    setOf("ACCEPTED") to { it.accepted },
    setOf("REJECTED") to { it.rejected },
    setOf("SENT", "SIGNED") to { it.sent },
    setOf("PENDING", "IN_PROCESS") to { it.inProcess },
    setOf("OPEN") to { it.shiftOpen },
    setOf("CLOSED") to { it.shiftClosed }
)

/** Действие названо словами, а не именем перечисления. */
fun actionTitle(code: String, texts: CabinetTexts): String = when (code) {
    "REGISTRATION" -> texts.registration
    "REREGISTRATION" -> texts.reregistration
    "DEREGISTRATION" -> texts.deregistration
    else -> code
}

/**
 * Поле карты, названное словом, а не именем контракта.
 *
 * Кабинет перечисляет изменённое кодами полей; владелец читает список
 * версий, чтобы понять, что именно переписала перерегистрация, и
 * `RETAIL_PLACE` ему об этом не говорит. Незнакомый код показывается
 * как пришёл: своего списка, расходящегося с кабинетом, здесь не заводят.
 */
fun cardFieldTitle(code: String, texts: CabinetTexts): String = when (code.uppercase()) {
    // Адрес карты — адрес торговой точки, записанный в КГД. Здесь стояла
    // подпись «Адрес кабинета» — та, которой на экране входа назван
    // сетевой адрес самой службы, — и список изменений карты сообщал,
    // что перерегистрация переписала адрес кабинета.
    "ADDRESS", "RKA", "CATO" -> texts.placeAddress
    "RETAIL_PLACE", "RETAILPLACE", "RETAIL_PLACE_ID" -> texts.placeName
    "MODEL", "KKM_MODEL", "MODEL_NAME" -> texts.model
    "FACTORY_NUMBER", "FACTORYNUMBER" -> texts.factoryNumber
    "REGISTRATION_NUMBER", "RNM" -> texts.registrationNumber
    "INTERNAL_NAME", "NAME" -> texts.internalName
    else -> code
}

/** Хвост кодов, означающих ожидание ответа ИСНА. */
private const val IN_PROCESS = "_IN_ISNA_PROCESS"

/**
 * Адрес на языке кабинета.
 *
 * Регистр отдаёт адрес по-русски и по-казахски, и в государственном
 * кабинете это не украшение: владелец, ведущий дела по-казахски, обязан
 * видеть казахский адрес — тот же, что уйдёт в заявление и в чек.
 */
fun addressIn(language: Language, russian: String?, kazakh: String?): String = when (language) {
    Language.Kk -> kazakh?.takeIf { it.isNotBlank() } ?: russian.orEmpty()
    else -> russian?.takeIf { it.isNotBlank() } ?: kazakh.orEmpty()
}
