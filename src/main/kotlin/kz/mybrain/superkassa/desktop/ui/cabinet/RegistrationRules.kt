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
        status == DEREGISTERED -> emptySet()
        status in ON_RECORD -> setOf(ActionKind.Reregistration, ActionKind.Deregistration)
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
fun tokenAllowed(register: CabinetRegister): Boolean = register.status in ON_RECORD

/** Состояния кассы, стоящей на учёте. */
private val ON_RECORD = setOf("REGISTERED", "REGISTERED_REREGISTRATION_SUCCESS")

/** Состояние снятой с учёта кассы. */
private const val DEREGISTERED = "DEREGISTERED"

/** Хвост состояний, означающих ожидание ответа ИСНА. */
private const val IN_ISNA_PROCESS = "_IN_ISNA_PROCESS"
