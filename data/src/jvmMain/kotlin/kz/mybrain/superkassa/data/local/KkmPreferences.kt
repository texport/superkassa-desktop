package kz.mybrain.superkassa.data.local

import java.io.File

/**
 * С какой кассой работает эта машина: выбранная касса, её отрасль
 * и данное ей здесь название.
 *
 * Отдельный предмет, потому что без выбора кассы кассира утром встречает
 * пустой список. Всё остальное можно потерять и не заметить.
 */
class KkmPreferences(private val kkmFile: File) {

    /** Касса, выбранная в прошлый раз. */
    var defaultKkmId: String?
        get() = readSetting(kkmFile)
        set(value) = writeSetting(kkmFile, value)

    /**
     * Адрес прежнего узла кассы, если его меняли с экрана настроек.
     *
     * Узла больше нет, а адрес нужен один раз — при переносе его данных:
     * узел, который ещё отвечает по этому адресу, пишет в свою базу,
     * и переносить её нельзя.
     */
    val formerNodeAddress: String
        get() = readSetting(nodeFile) ?: FORMER_NODE_ADDRESS

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

    private companion object {
        /** Адрес, по которому узел отвечал, пока его не меняли. */
        const val FORMER_NODE_ADDRESS = "http://127.0.0.1:8080"
    }
}
