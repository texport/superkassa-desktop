package kz.mybrain.superkassa.data.print

import kz.mybrain.superkassa.domain.print.model.Kept
import kz.mybrain.superkassa.domain.print.port.PrintOut

/**
 * Печать на Android: принтера у кассы на телефоне пока нет.
 *
 * Порт без реализации отказывает честно: принтеров касса не видит, и
 * печать отвечает «принтера нет», а не «напечатано»; сохранение в файл
 * отвечает «на этом устройстве некуда», а не молчит, как передумавший
 * владелец. Лента показывается такой, какой её нарисовала касса.
 */
class NoPrintOut : PrintOut {
    override suspend fun printers(): List<String> = emptyList()

    override suspend fun tape(png: ByteArray): ByteArray = png

    override suspend fun print(tape: ByteArray, printer: String?, widthMm: Int, copies: Int): Boolean = false

    override suspend fun keep(bytes: ByteArray, name: String, title: String): Kept = Kept.Unavailable
}
