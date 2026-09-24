package kz.mybrain.superkassa.presentation.print.preview

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse
import kz.mybrain.superkassa.domain.print.model.PrintSource

/**
 * Что можно сделать с печатной формой — из любого раздела.
 *
 * Разделы зовут просмотр и печать отсюда, а не держат свою печать:
 * форма одна на окно, живёт над всеми разделами и переживает уход
 * кассира в продажу. Действия по умолчанию пустые — для снимков вида.
 */
interface PrintActions {

    /** Форма документа журнала; `null` — документа нет, и так и сказано. */
    fun preview(documentId: String?, file: String? = null) = Unit

    /** Форма документа кассы: имя файла складывается из вида и номера. */
    fun preview(document: FiscalDocumentResponse) = preview(document.id, PrintFileName.of(document))

    /** Форма документа кабинета по пакету протокола. */
    fun previewPacket(packet: String, name: String, file: String? = null) = Unit

    /** Печать документа журнала без просмотра. */
    fun print(documentId: String) = Unit

    /** Печать документа кабинета без просмотра. */
    fun printPacket(packet: String) = Unit
}

/**
 * Что можно сделать в самом окне печатной формы и в окне пина.
 *
 * Действия по умолчанию пустые — для снимков вида.
 */
interface PaperActions {

    /** Печать открытой формы: касса для этого уже не нужна. */
    fun printShown() = Unit

    /** Сохранение открытой формы в файл выбранного вида. */
    fun saveShown() = Unit

    /** Повтор того, на чём касса отказала. */
    fun retry() = Unit

    fun close() = Unit

    /** Владелец ввёл пин: работа продолжается с того места, где встала. */
    fun enterPin(pin: String) = Unit

    fun cancelPin() = Unit
}

/** Действия печати, выполняемые этой моделью. */
fun PrintViewModel.actions(): PrintActions {
    val model = this
    return object : PrintActions {
        override fun preview(documentId: String?, file: String?) =
            model.preview(documentId?.let(PrintSource::Journal), file)

        override fun previewPacket(packet: String, name: String, file: String?) =
            model.preview(PrintSource.Packet(packet, name), file)

        override fun print(documentId: String) = model.print(PrintSource.Journal(documentId))

        override fun printPacket(packet: String) = model.print(PrintSource.Packet(packet, PACKET))
    }
}

/** Действия окна печатной формы, выполняемые этой моделью. */
fun PrintViewModel.paperActions(): PaperActions {
    val model = this
    return object : PaperActions {
        override fun printShown() = model.printShown()

        override fun saveShown() = model.saveShown()

        override fun retry() = model.retry()

        override fun close() = model.close()

        override fun enterPin(pin: String) = model.enterPin(pin)

        override fun cancelPin() = model.cancelPin()
    }
}

/** Печать окна для разделов: даёт её каркас окна. */
val LocalPrint = staticCompositionLocalOf<PrintActions> { object : PrintActions {} }

/** Имя документа, напечатанного без просмотра: файла у него не будет. */
private const val PACKET = "document"
