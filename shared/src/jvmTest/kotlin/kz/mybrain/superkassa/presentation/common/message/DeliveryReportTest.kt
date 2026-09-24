package kz.mybrain.superkassa.presentation.common.message

import io.github.texport.superkassa.core.presentation.api.model.ofd.DeliveryStatus
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertTrue

/** Итог фискального действия читается предложениями: после точки — с заглавной. */
class DeliveryReportTest {

    @Test
    fun `после точки итог продолжается с заглавной на всех языках`() {
        Language.entries.forEach { language ->
            val texts = textsOf(language).common
            listOf(DeliveryStatus.ONLINE_ERROR, DeliveryStatus.NOT_SENT).forEach { status ->
                val report = deliveryReport("Смена закрыта", status, texts)
                val next = report.substringAfter(". ").first()
                assertTrue(next.isUpperCase(), "$language $status: «$report»")
            }
        }
    }
}
