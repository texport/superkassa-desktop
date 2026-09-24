package kz.mybrain.superkassa.presentation.journal.documents

import io.github.texport.superkassa.core.presentation.api.model.receipt.DocumentType
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Вид документа словами и тогда, когда справочник кассы ещё не прочитан.
 *
 * Справочник журнал читает сам после открытия, и до его ответа — или
 * если касса его не отдала — строки стояли кодами: `SALE`, `RETURN`,
 * `BUY`, `Z_REPORT`, в отборе по виду — те же коды.
 */
class DocumentTypeNamesTest {

    @Test
    fun `ни один вид документа ядра не показан кодом без справочника`() {
        val codes = DocumentType.entries.map { it.name } + "Z_REPORT"
        Language.entries.forEach { language ->
            val enums = textsOf(language).common.enums
            codes.forEach { code ->
                val title = documentTypeTitle(code, emptyMap(), language, enums)
                assertTrue(title != code, "$language: вид $code показан кодом")
            }
        }
    }
}
