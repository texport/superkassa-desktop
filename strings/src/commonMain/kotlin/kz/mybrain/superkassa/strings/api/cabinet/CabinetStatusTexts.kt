package kz.mybrain.superkassa.strings.api.cabinet

/**
 * Названия состояний кассы, заявления и смены.
 *
 * Запасная таблица приложения: состояния приходят от кабинета протокольными
 * кодами — `DRAFT`, `KKM_ACTIVE`, `REREGISTRATION_IN_ISNA_PROCESS`, — и пока
 * кода нет в таблице, на экране стоял он сам. Владельцу «DRAFT» не говорит
 * ничего, и по правилам приложения кодов на экране быть не должно: их место
 * в журнале поддержки.
 *
 * Отдельной группой, а не в общем наборе: таблицу правят целиком, когда
 * кабинет заводит новое состояние, и искать её среди двух с половиной сотен
 * полей набора не нужно.
 *
 * @param unknown состояние, которого в таблице нет: названо словами, чтобы
 *   незнакомый код не доходил до экрана.
 */
data class CabinetStatusTexts(
    val draft: String,
    val registered: String,
    val deregistered: String,
    val active: String,
    val inactive: String,
    val blocked: String,
    val accepted: String,
    val rejected: String,
    val sent: String,
    val inProcess: String,
    val shiftOpen: String,
    val shiftClosed: String,
    val unknown: String
)
