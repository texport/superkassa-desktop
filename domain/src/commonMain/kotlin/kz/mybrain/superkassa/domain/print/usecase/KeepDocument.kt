package kz.mybrain.superkassa.domain.print.usecase

import kz.mybrain.superkassa.domain.print.model.Kept
import kz.mybrain.superkassa.domain.print.port.PrintOut

/** Кладёт форму в файл, который выберет владелец. */
class KeepDocument(private val printOut: PrintOut) {

    /**
     * @param name имя файла: вид документа и номер, а не внутренний идентификатор.
     * @param title заголовок окна выбора файла.
     */
    suspend operator fun invoke(bytes: ByteArray, name: String, title: String): Kept =
        printOut.keep(bytes, name, title)
}
