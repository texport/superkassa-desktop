package kz.mybrain.superkassa.desktop.app

import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.server.updateKkmName

/**
 * Название кассы: узел — место, где оно живёт.
 *
 * Названия хранило рабочее место, и владелец, назвавший кассу, видел
 * название только за своей машиной; за соседней кассу по-прежнему звали
 * регистрационным номером. Теперь название пишется узлу, а рабочее место
 * держит своё только как запасное — на случай, когда узел его не принял.
 */

/**
 * Переименовывает кассу.
 *
 * Отказ узла показывается владельцу его же словами: он нажал «Сохранить»
 * и обязан узнать, что название не записано.
 *
 * @return `true`, если название сохранено на узле.
 */
suspend fun Session.rename(kkm: Kkm, name: String?): Boolean {
    val entered = pin
    val saved = entered.isNotBlank() &&
        guard(texts.settings.localName) { client.updateKkmName(kkm.kkmId, name, entered) } != null
    // Своё название снимается: иначе оно перекрывало бы только что
    // записанное на узел и правка выглядела бы непринятой.
    keepNameLocally(kkm, if (saved) null else name)
    if (saved) refreshKkms()
    return saved
}

/**
 * Переносит названия касс из кабинета на узел.
 *
 * Однократный вход владельца в кабинет называет кассы для всех рабочих
 * мест. Кассы, у которых название уже есть, не трогаются.
 */
suspend fun Session.adoptCabinetNames(registers: List<CabinetRegister>) {
    val unnamed = unnamedFromCabinet(registers)
    if (unnamed.isEmpty()) return
    var stored = false
    unnamed.forEach { (kkm, name) ->
        if (sendName(kkm, name)) stored = true else keepNameLocally(kkm, name)
    }
    if (stored) refreshKkms()
}

/**
 * Пишет название на узел молча.
 *
 * Перенос из кабинета владелец не затевал — он просто вошёл. Красная
 * строка об отказе на этом месте объясняла бы кассиру то, чего он
 * не делал; отказ уходит в журнал, а название остаётся на рабочем месте.
 */
private suspend fun Session.sendName(kkm: Kkm, name: String): Boolean {
    val entered = pin
    if (entered.isBlank()) return false
    return quietly(NAMING) { client.updateKkmName(kkm.kkmId, name, entered) } != null
}

/** Чем подписан перенос названий в журнале. */
private const val NAMING = "название кассы из кабинета"
