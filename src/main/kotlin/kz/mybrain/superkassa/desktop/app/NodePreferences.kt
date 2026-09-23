package kz.mybrain.superkassa.desktop.app

import kz.mybrain.superkassa.desktop.server.ServerClient
import java.io.File

/**
 * С каким узлом и с какой кассой работает эта машина.
 *
 * Отдельный предмет, потому что это единственная настройка рабочего места,
 * без которой касса не работает вовсе: адрес узла, выбранная за ним касса
 * и данное ей здесь название. Всё остальное можно потерять и не заметить,
 * а потерянный выбор кассы встречает кассира утром пустым списком.
 */
class NodePreferences(private val kkmFile: File) {

    /** Касса, выбранная в прошлый раз. */
    var defaultKkmId: String?
        get() = readSetting(kkmFile)
        set(value) = writeSetting(kkmFile, value)

    /**
     * Адрес узла, с которым работает касса.
     *
     * Узел обычно стоит на этой же машине, но за прилавком бывает иначе:
     * один узел на несколько рабочих мест. Прежде адрес был зашит,
     * и такой кассе оставалось только не работать.
     */
    var url: String
        get() = readSetting(nodeFile) ?: ServerClient.DEFAULT_URL
        set(value) = writeSetting(nodeFile, value.trim().takeIf { it.isNotBlank() })

    /**
     * Вид отрасли, в которой работает касса.
     *
     * Отрасль у кассы одна: на заправке не бывает чеков стоянки, а
     * в магазине — чеков такси. Поэтому она стоит здесь, рядом с самой
     * кассой, а не спрашивается в каждом чеке. Пустая настройка означает
     * торговлю — с ней касса и работала до появления выбора.
     *
     * Хранится за каждой кассой, как и её название на этом рабочем месте:
     * за одной машиной работают несколько касс, и отрасли у них бывают
     * разные.
     */
    fun domain(kkmId: String): String? = readSetting(domainFile(kkmId))

    fun chooseDomain(kkmId: String, code: String?) = writeSetting(domainFile(kkmId), code)

    /**
     * Своё название кассы на этом рабочем месте.
     *
     * Все сведения о кассе — регистрационный номер, организация, адрес —
     * приходят от ОФД и здесь не меняются. Переименовать можно только
     * для себя: «Касса у входа» понятнее номера, когда их в зале четыре.
     */
    fun localName(kkmId: String): String? = readSetting(nameFile(kkmId))

    fun rename(kkmId: String, name: String?) = writeSetting(nameFile(kkmId), name)

    private val directory: File? = kkmFile.parentFile

    private val nodeFile = File(directory, "node")

    private fun domainFile(kkmId: String) = File(directory, "domains/$kkmId")

    private fun nameFile(kkmId: String) = File(directory, "names/$kkmId")
}
