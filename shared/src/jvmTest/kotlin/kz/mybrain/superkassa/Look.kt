package kz.mybrain.superkassa

import kz.mybrain.superkassa.presentation.strings.cabinet.cabinetTexts
import kz.mybrain.superkassa.presentation.strings.common.Language
import java.io.File
import kotlin.test.assertTrue

/**
 * Общая оснастка снимков: куда класть снимок, рабочее окно без сети
 * и надписи кабинета.
 *
 * Снимки смотрит человек, и собирать их состав в каждом наборе заново
 * значит расходиться в мелочах. Ни одного обращения в сеть: на снимке
 * нужно ровно то, что владелец видит без связи.
 */
internal object Look {

    val cabinet = cabinetTexts(Language.Ru)

    fun shot(name: String, bytes: ByteArray) {
        val file = File("/tmp/$name.png")
        file.writeBytes(bytes)
        assertTrue(file.length() > 0, "снимок $name пуст")
    }

    /** Рабочее окно снимка: касса без сети, вход кассира уже сделан. */
    fun desk(): KassaDesk = KassaScene.desk()
}
