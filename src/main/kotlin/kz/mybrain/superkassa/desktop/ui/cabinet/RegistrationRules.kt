package kz.mybrain.superkassa.desktop.ui.cabinet

import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts

/**
 * Какое заявление в ИСНА по кассе сейчас подаётся.
 *
 * Прежде все три предлагались всегда, и кассу, стоящую на учёте, можно
 * было отправить ставить на учёт заново. Отказ приходил из ИСНА через
 * минуты — и приходил кодом, из которого владельцу нечего понять.
 *
 * Правила читаются сверху вниз:
 *
 * 1. Ждём ответа ИСНА — не подаётся ничего: заявление уже в работе,
 *    и второе рядом с ним ИСНА не примет.
 * 2. Касса снята с учёта — не подаётся ничего: её больше нет в реестре.
 * 3. Касса на учёте — перерегистрация и снятие; ставить на учёт нечего.
 * 4. Иначе (черновик или отказ ИСНА) — только постановка на учёт.
 */
fun availableActions(register: CabinetRegister): Set<ActionKind> {
    val status = register.status
    return when {
        status.endsWith(IN_ISNA_PROCESS) -> emptySet()
        kkmRecord(status) == KkmRecord.Deregistered -> emptySet()
        onRecord(status) -> setOf(ActionKind.Reregistration, ActionKind.Deregistration)
        else -> setOf(ActionKind.Registration)
    }
}

/**
 * Почему по кассе сейчас не подаётся ни одно заявление.
 *
 * Пустой ряд погашенных сегментов молчит о причине: владелец видит, что
 * нажать нечего, и не знает, ждать ему или что-то делать.
 */
fun noActionsReason(register: CabinetRegister, texts: CabinetTexts): String =
    if (register.status.endsWith(IN_ISNA_PROCESS)) {
        texts.applicationInFlight
    } else {
        texts.noApplications
    }

/**
 * Ждёт ли касса ответа ИСНА по поданному заявлению.
 *
 * Пока ждёт, кабинет держит кассу снятой с обслуживания, а карточка
 * перечитывается сама: ответ приходит через десятки секунд.
 */
fun awaitingIsna(register: CabinetRegister): Boolean = register.status.endsWith(IN_ISNA_PROCESS)

/**
 * Выдаётся ли сейчас токен.
 *
 * Кабинет выдаёт его кассе, стоящей на учёте, и только когда по ней нет
 * поданного заявления. Прежде кнопка нажималась всегда, и по черновику
 * приходил отказ — по-английски и кодом.
 */
fun tokenAllowed(register: CabinetRegister): Boolean = onRecord(register.status)

/**
 * Правится ли касса в кабинете: заводской номер и удаление.
 *
 * Правится та, по которой в КГД ещё ничего не ушло, — и та, которой
 * КГД отказал: её заводской номер и надо исправить, чтобы подать
 * заявление заново. Прежде здесь стояло одно условие — нет номера КГД, —
 * а номера нет и у кассы, чьё заявление КГД сейчас рассматривает:
 * заводской номер правился прямо в рассматриваемом заявлении, а кнопка
 * удаления снимала кассу, о которой уже спрошено.
 */
fun editableInCabinet(register: CabinetRegister): Boolean =
    !awaitingIsna(register) && register.registrationNumber.isNullOrBlank()

/** Хвост состояний, означающих ожидание ответа ИСНА. */
private const val IN_ISNA_PROCESS = "_IN_ISNA_PROCESS"
