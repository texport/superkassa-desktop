package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.desktop.ui.components.Chip
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
 * жёлтый — ждём ответа, красный — отказ.
 */
@Composable
fun CabinetStatusChip(status: String?, texts: CabinetTexts) {
    val code = status?.takeIf { it.isNotBlank() } ?: return
    Chip(text = statusTitle(code, texts), color = statusColor(code))
}

/** Название состояния словами; незнакомый код доходит как есть. */
fun statusTitle(code: String, texts: CabinetTexts): String = when (code) {
    "DRAFT" -> texts.statusDraft
    "REGISTERED", "REGISTERED_REREGISTRATION_SUCCESS" -> texts.statusRegistered
    "DEREGISTERED" -> texts.statusDeregistered
    "KKM_ACTIVE" -> texts.statusActive
    "KKM_INACTIVE" -> texts.statusInactive
    "ACCEPTED" -> texts.statusAccepted
    "REJECTED" -> texts.statusRejected
    "SENT", "SIGNED" -> texts.statusSent
    "OPEN" -> texts.shiftOpen
    "CLOSED" -> texts.shiftClosed
    else -> if (code.endsWith(IN_PROCESS)) texts.statusInProcess else code
}

/** Цвет состояния: сделано, ожидание или отказ. */
@Composable
fun statusColor(code: String): Color = when {
    code in DONE -> StatusColors.delivered
    code in REFUSED -> StatusColors.refused
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
private val REFUSED = setOf("REJECTED", "REGISTRATION_IN_ISNA_ERROR", "DEREGISTERED")

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
