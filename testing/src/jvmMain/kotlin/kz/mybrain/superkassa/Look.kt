package kz.mybrain.superkassa

import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import java.io.File
import kotlin.test.assertTrue

/**
 * Общая оснастка снимков: куда класть снимок и надписи кабинета.
 *
 * Снимки смотрит человек, и собирать их состав в каждом наборе заново
 * значит расходиться в мелочах. Ни одного обращения в сеть: на снимке
 * нужно ровно то, что владелец видит без связи.
 */
object Look {

    /** Надписи кабинета на русском — языке снимков. */
    val cabinet = textsOf(Language.Ru).cabinet

    /** Кадр [bytes] в `/tmp/[name].png`; пустой кадр — провал проверки. */
    fun shot(name: String, bytes: ByteArray) {
        val file = File("/tmp/$name.png")
        file.writeBytes(bytes)
        assertTrue(file.length() > 0, "снимок $name пуст")
    }
}
