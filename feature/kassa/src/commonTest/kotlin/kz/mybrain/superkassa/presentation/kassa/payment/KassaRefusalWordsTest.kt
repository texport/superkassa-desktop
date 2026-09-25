package kz.mybrain.superkassa.presentation.kassa.payment

import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.model.sale.UNKNOWN_VAT
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.Test
import kotlin.test.assertEquals

/** Какими словами назван отказ кассы: её, если они годятся кассиру, иначе своими по коду. */
class KassaRefusalWordsTest {

    private val noCash = Answer.Refused(
        "INSUFFICIENT_CASH",
        "В кассе недостаточно наличных.",
        "Кассада қолма-қол ақша жеткіліксіз.",
        "Not enough cash in the drawer."
    )

    @Test
    fun `понятные слова ядра остаются на каждом языке`() {
        assertEquals(noCash.ru, kassaRefusalWords(noCash, Language.Ru))
        assertEquals(noCash.kk, kassaRefusalWords(noCash, Language.Kk))
        assertEquals(noCash.en, kassaRefusalWords(noCash, Language.En))
    }

    @Test
    fun `слова с кодом ставки — свои`() {
        val vat = Answer.Refused(
            "RECEIPT_VAT_NOT_ALLOWED",
            "Касса не является плательщиком НДС: ставка VAT_12 в чеке недопустима",
            "Касса ҚҚС төлеушісі емес: чектегі VAT_12 мөлшерлемесі жарамсыз",
            "The register is not a VAT payer: rate VAT_12 is not allowed in a receipt"
        )
        Language.entries.forEach { language ->
            assertEquals(textsOf(language).kassa.refusal.vatNotPayer, kassaRefusalWords(vat, language))
        }
    }

    /**
     * Код неизвестной ставки ставит домен, а слова к нему — только здесь:
     * у текстов отказа этого кода нет, и разойтись с доменом ему не с чем.
     */
    @Test
    fun `отказ до кассы без слов — свои по коду домена`() {
        val unknown = Answer.Refused(UNKNOWN_VAT, "", "", "")

        Language.entries.forEach { language ->
            assertEquals(textsOf(language).kassa.refusal.vatUnknown, kassaRefusalWords(unknown, language))
        }
    }

    @Test
    fun `слов на языке интерфейса нет — общая фраза с кодом`() {
        val english = Answer.Refused("SOMETHING_NEW", "Something new", "Something new", "Something new")

        assertEquals("Касса отказала, код отказа SOMETHING_NEW", kassaRefusalWords(english, Language.Ru))
        assertEquals("Something new", kassaRefusalWords(english, Language.En))
    }
}
