package kz.mybrain.superkassa.desktop.ui.cabinet

import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts

/**
 * Что шапка окна говорит о кабинете.
 *
 * @param title где владелец сейчас находится.
 * @param subtitle чьё это: владелец компании или касса, чьи документы открыты.
 * @param inDocuments открыты ли документы кассы. По этому признаку шапка
 *   решает, куда ведёт возврат: наружу из кабинета или назад к карточке
 *   кассы, из которой документы открыли.
 */
data class CabinetHead(val title: String, val subtitle: String?, val inDocuments: Boolean)

/**
 * Где владелец в кабинете.
 *
 * Навигация в приложении одна и живёт в шапке окна, поэтому и знать, где
 * владелец, обязана шапка, а не экран под ней. Пока документы кассы
 * открыты, шапка называет их и кассу; до входа по ЭЦП называть нечего —
 * ни компании, ни владельца ещё нет.
 *
 * @param company название компании; пусто до входа по ЭЦП.
 * @param owner строка о вошедшем владельце; `null`, пока он не вошёл.
 * @param documentsTitle название экрана документов кассы — то же, что
 *   и в журнале кассы: экран у них один.
 */
fun cabinetHead(
    register: CabinetRegister?,
    company: String?,
    owner: String?,
    texts: CabinetTexts,
    documentsTitle: String
): CabinetHead = when (register) {
    null -> CabinetHead(company?.takeIf { it.isNotBlank() } ?: texts.title, owner, inDocuments = false)
    else -> CabinetHead(documentsTitle, registerTitle(register), inDocuments = true)
}
