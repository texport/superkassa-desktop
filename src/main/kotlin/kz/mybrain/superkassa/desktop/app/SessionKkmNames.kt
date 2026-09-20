package kz.mybrain.superkassa.desktop.app

import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister

/**
 * Откуда рабочее место берёт название кассы.
 *
 * Отдельный предмет, потому что название приходит с трёх сторон: его даёт
 * владелец в кабинете, его же хранит узел, и здесь остаётся своё — на случай,
 * когда узел название не принял. Правила, чьё название побеждает, собраны
 * тут; сеанс наружу отдаёт только готовое [Session.displayName].
 */

/**
 * Название, которое узел не принял, остаётся хотя бы здесь.
 *
 * Запасной путь, а не второе место хранения: без него отказ узла
 * оставлял бы кассу на экране входа одним регистрационным номером.
 */
internal fun Session.keepNameLocally(kkm: Kkm, name: String?) = settings.rename(kkm, name)

/**
 * Подбирает кассам узла названия, данные им в кабинете.
 *
 * Касса кабинета опознаётся регистрационным номером КГД, а у не
 * поставленной на учёт его ещё нет — тогда по идентификатору ОФД.
 * Возвращаются только безымянные: названное здесь руками и названное
 * на узле кабинетом не затирается.
 */
internal fun Session.unnamedFromCabinet(registers: List<CabinetRegister>): List<Pair<Kkm, String>> {
    val named = registers.mapNotNull { register ->
        val name = register.internalName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
        val key = register.registrationNumber?.takeIf { it.isNotBlank() } ?: register.kkmId.toString()
        key to name
    }.toMap()
    return settings.matchCabinetNames(named, kkms)
}
