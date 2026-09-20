package kz.mybrain.superkassa.desktop.ui.cabinet

import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.cabinet.TechnicalState
import kz.mybrain.superkassa.desktop.ui.users.UserRules

/**
 * Работает ли касса кабинета на этой машине.
 *
 * Узел знает только свои кассы: заведена ли эта касса на соседней машине,
 * ему неизвестно, и утверждать про неё нельзя ничего. Поэтому состояний
 * ровно три, и второе с третьим различает не узел, а учёт в КГД.
 */
sealed interface NodeWork {

    /** Заведена здесь: узел знает её под этим идентификатором ОФД. */
    data class Here(val kkm: Kkm) : NodeWork

    /** Не стоит на учёте в КГД: работать с неё нельзя ни на какой машине. */
    data object NotOnRecord : NodeWork

    /** На учёте, а на этой машине не заведена — единственное состояние с действием. */
    data object Absent : NodeWork
}

/**
 * В каком из трёх состояний касса кабинета на этой машине.
 *
 * Сверка идёт по идентификатору кассы у ОФД: в кабинете это `kkmId`,
 * на узле — `ofdSystemId`, и других общих ключей у них нет. Заводской
 * номер сюда не годится: у кассы, поставленной на учёт по чужому номеру,
 * он совпал бы с чужим.
 */
fun nodeWork(register: CabinetRegister, kkms: List<Kkm>): NodeWork {
    val here = kkms.firstOrNull { it.ofdSystemId == register.kkmId.toString() }
    return when {
        here != null -> NodeWork.Here(here)
        !onRecord(register) -> NodeWork.NotOnRecord
        else -> NodeWork.Absent
    }
}

/**
 * Стоит ли касса на учёте.
 *
 * Номер и состояние проверяются оба: черновик с приписанным номером
 * так же не годится, как стоящая в очереди на снятие касса без него.
 */
private fun onRecord(register: CabinetRegister): Boolean =
    !register.registrationNumber.isNullOrBlank() && tokenAllowed(register)

/**
 * Слышал ли ОФД эту кассу.
 *
 * Признак того, что касса уже где-то работает: ОФД принимал от неё данные
 * или держит на ней открытую смену. Перевыпуск токена такую кассу
 * остановит, и владелец обязан это подтвердить отдельно.
 */
fun heardElsewhere(technical: TechnicalState?): Boolean =
    !technical?.lastContactAt.isNullOrBlank() || technical?.shiftStatus == SHIFT_OPEN

/** Заполненное владельцем в окне заведения. */
data class AdoptForm(
    /** ОФД и контур выбраны, а у своего адреса заданы хост и порт. */
    val ofdComplete: Boolean,
    val adminPin: String,
    val handoverNeeded: Boolean,
    val handoverAccepted: Boolean
)

/** Подписи полей окна: они объявлены в наборах надписей, а не здесь. */
data class AdoptLabels(val ofd: String, val adminPin: String, val handover: String)

/**
 * Чего не хватает, чтобы завести кассу на этой машине.
 *
 * Пустой список открывает действие. Отметка о последствии стоит в этом же
 * перечне наравне с полями: без неё действие недоступно так же, как без
 * пина, — касса, которую ОФД сейчас слышит, работает на другой машине,
 * и перевыпуск токена её остановит.
 */
fun adoptMissing(form: AdoptForm, labels: AdoptLabels): List<String> = listOfNotNull(
    labels.ofd.takeUnless { form.ofdComplete },
    labels.adminPin.takeUnless { UserRules.pinAccepted(form.adminPin) },
    labels.handover.takeIf { form.handoverNeeded && !form.handoverAccepted }
)

/** Состояние открытой смены, каким его отдаёт сервер приёма данных. */
private const val SHIFT_OPEN = "OPEN"
