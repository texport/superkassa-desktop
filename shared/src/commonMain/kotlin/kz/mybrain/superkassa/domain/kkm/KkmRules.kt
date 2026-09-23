package kz.mybrain.superkassa.domain.kkm

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmState

/**
 * Правила о кассе, какой её отдаёт ядро.
 *
 * Модель — ответ фасада ядра как есть: своей копии кассы у приложения нет,
 * а то, что экраны о ней спрашивают, собрано здесь, один раз.
 */

/**
 * Имя кассы для кассира, когда своего названия у неё нет.
 *
 * Название организации на всех кассах одно, и в списке из десяти строк
 * своей по нему не найти. Опознаётся касса регистрационным номером КГД —
 * он написан на самой машине и стоит в чеке; заводской номер и внутренний
 * код идут запасными.
 */
val KkmResponse.title: String
    get() = kkmKgdId?.takeIf { it.isNotBlank() }
        ?: factoryNumber?.takeIf { it.isNotBlank() }
        ?: kkmId.take(SHORT_ID)

/**
 * Как зовут кассу на этом рабочем месте.
 *
 * Порядок один на всё приложение: своё название рабочего места — его
 * задали здесь и руками, — затем название, данное владельцем, затем [title].
 */
fun KkmResponse.displayName(localName: String?): String =
    localName?.takeIf { it.isNotBlank() } ?: name?.takeIf { it.isNotBlank() } ?: title

/** Организация, от имени которой касса работает; `null` — сведений нет. */
val KkmResponse.orgTitle: String?
    get() = ofdServiceInfo?.orgTitle?.takeIf { it.isNotBlank() && it != UNKNOWN }

/** Адрес установки: две кассы одной организации различают по нему. */
val KkmResponse.orgAddress: String?
    get() = ofdServiceInfo?.orgAddress?.takeIf { it.isNotBlank() && it != UNKNOWN }

/** Подходит ли касса под набранное кассиром: номера, организация, адрес, название. */
fun KkmResponse.matches(query: String, localName: String?): Boolean {
    val needle = query.trim()
    if (needle.isEmpty()) return true
    return listOfNotNull(kkmKgdId, factoryNumber, ofdSystemId, kkmId, name, orgTitle, orgAddress, localName)
        .any { it.contains(needle, ignoreCase = true) }
}

/** Касса заблокирована — и снятая с учёта тоже: фискальных команд не принимает. */
val KkmResponse.isBlocked: Boolean
    get() = state == KkmState.BLOCKED.name

/**
 * Касса в режиме программирования.
 *
 * Фискальных команд она в этом состоянии не принимает: «смена закрыта»
 * на экране в этот момент — не состояние смены, а отказ её читать.
 */
val KkmResponse.isProgramming: Boolean
    get() = isProgrammingMode || state == KkmState.PROGRAMMING.name

/**
 * Касса работает автономно: есть неотправленное и отметка о начале.
 *
 * Отметку касса снимает при следующей фискальной операции, и без очереди
 * касса с пустой очередью выглядела бы автономной, хотя связь вернулась.
 */
val KkmResponse.isAutonomous: Boolean
    get() = autonomousSince != null && offlineQueueCount > 0

/** Сколько знаков внутреннего кода хватает, чтобы различить кассы. */
private const val SHORT_ID = 8

/** Так ядро заполняет сведения об организации, которых ОФД ещё не прислал. */
private const val UNKNOWN = "UNKNOWN"
