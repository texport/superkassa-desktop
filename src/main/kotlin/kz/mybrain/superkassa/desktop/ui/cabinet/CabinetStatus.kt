package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.StatusTone
import kz.mybrain.superkassa.desktop.ui.components.toneColor
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Состояния кабинета словами и цветом.
 *
 * Кабинет отдаёт протокольные коды — `DRAFT`, `REGISTERED`,
 * `REREGISTRATION_IN_ISNA_PROCESS`. Владельцу они не говорят ничего, а по
 * правилам приложения кодов на экране быть не должно: их место в журнале
 * поддержки, а не в карточке кассы.
 *
 * Цвет несёт тот же смысл, что и везде в приложении: зелёный — сделано,
 * жёлтый — ждём ответа, красный — отказ, серый — законченное дело,
 * в котором ничего не случилось.
 */
@Composable
fun CabinetStatusChip(status: String?, texts: CabinetTexts) {
    val code = status?.takeIf { it.isNotBlank() } ?: return
    Chip(text = statusTitle(code, texts), color = statusColor(code))
}

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
fun statusWords(code: String, texts: CabinetTexts): String? = when (code.uppercase()) {
    "DRAFT" -> texts.statuses.draft
    "REGISTERED", "REGISTERED_REREGISTRATION_SUCCESS" -> texts.statuses.registered
    "DEREGISTERED" -> texts.statuses.deregistered
    // Три отказных состояния учёта не назывались никак и доходили
    // до экрана как «состояние неизвестно» — жёлтым, будто их ещё ждут.
    "REGISTRATION_IN_ISNA_ERROR",
    "REGISTERED_REREGISTRATION_ERROR",
    "DEREGISTRATION_ERROR" -> texts.statuses.rejected
    "ACTIVE", "KKM_ACTIVE" -> texts.statuses.active
    "INACTIVE", "KKM_INACTIVE" -> texts.statuses.inactive
    "BLOCKED", "KKM_BLOCKED" -> texts.statuses.blocked
    "ACCEPTED" -> texts.statuses.accepted
    "REJECTED" -> texts.statuses.rejected
    "SENT", "SIGNED" -> texts.statuses.sent
    "PENDING", "IN_PROCESS" -> texts.statuses.inProcess
    "OPEN" -> texts.statuses.shiftOpen
    "CLOSED" -> texts.statuses.shiftClosed
    else -> texts.statuses.inProcess.takeIf { code.endsWith(IN_PROCESS) }
}

/** Цвет состояния: сделано, законченное дело, отказ или ожидание. */
@Composable
fun statusColor(code: String): Color = when {
    code in DONE -> StatusColors.delivered
    code in REFUSED -> StatusColors.refused
    code in SETTLED -> toneColor(StatusTone.Idle)
    else -> StatusColors.pending
}

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

/** Состояния, означающие сделанное. */
private val DONE = setOf("REGISTERED", "REGISTERED_REREGISTRATION_SUCCESS", "KKM_ACTIVE", "ACCEPTED", "OPEN")

/** Состояния, означающие отказ. */
private val REFUSED = setOf(
    "REJECTED",
    "REGISTRATION_IN_ISNA_ERROR",
    "REGISTERED_REREGISTRATION_ERROR",
    "DEREGISTRATION_ERROR"
)

/**
 * Состояния законченного дела: не удача и не беда.
 *
 * Касса, снятая с учёта по заявлению самого владельца, стояла в списке
 * красной плашкой рядом с работающими — и выглядела сломанной. Снятие
 * с учёта владелец затеял сам и довёл до конца; чинить здесь нечего.
 */
private val SETTLED = setOf("DEREGISTERED")

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
