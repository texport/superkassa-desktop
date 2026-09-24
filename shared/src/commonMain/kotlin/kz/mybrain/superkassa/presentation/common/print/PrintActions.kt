package kz.mybrain.superkassa.presentation.common.print

import androidx.compose.runtime.staticCompositionLocalOf
import io.github.texport.superkassa.core.presentation.api.model.kkm.FiscalDocumentResponse

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

/** Печать окна для разделов: даёт её каркас окна. */
val LocalPrint = staticCompositionLocalOf<PrintActions> { object : PrintActions {} }
