package kz.mybrain.superkassa

import kz.mybrain.superkassa.data.node.DictionaryEntry
import kz.mybrain.superkassa.data.node.Document
import kz.mybrain.superkassa.data.node.Kkm
import kz.mybrain.superkassa.data.node.OrgInfo
import kz.mybrain.superkassa.data.node.SoldItem
import kz.mybrain.superkassa.presentation.sale.Basket
import kz.mybrain.superkassa.presentation.sale.Position
import kz.mybrain.superkassa.presentation.sale.SaleForm
import kz.mybrain.superkassa.presentation.strings.Language
import kz.mybrain.superkassa.presentation.theme.Look
import kz.mybrain.superkassa.presentation.theme.TextScale
import java.math.BigDecimal

/**
 * Предельные данные кассовых экранов.
 *
 * Разметку ломает не обычный чек, а крайний: наименование классификатора
 * в двести знаков, казахское слово без пробелов, сумма в миллиарды, чек
 * на тысячу строк. Всё это собрано здесь один раз, чтобы экраны проверялись
 * на одном и том же.
 */
internal object KassaExtremes {

    /** Наименование на двести знаков — формулировка классификатора целиком. */
    val LONG_NAME = (
        "Говядина охлаждённая высшего сорта, вырезка без кости, фасованная в вакуумную упаковку " +
            "по технологии длительного хранения, для ресторанов и магазинов сети, партия номер 000417"
        ).repeat(2).take(NAME_LENGTH)

    /** Длинное казахское слово без единого пробела. */
    const val KAZAKH_WORD = "Мемлекеттікбағдарламаныңорындалуынқамтамасызетушілердің"

    /** Сумма в миллиарды тенге. */
    val BILLIONS = BigDecimal("1234567890.12")

    /** Окна: наименьшее, по умолчанию, мониторы и планшеты стоймя и лёжа. */
    val WINDOWS = listOf(960 to 640, 1180 to 820, 1920 to 1080, 2560 to 1080, 800 to 1280, 1280 to 800)

    /** Ступени шрифта и языки, на которых меряются экраны. */
    val SCALES = listOf(TextScale.Normal, TextScale.Larger)
    val LANGUAGES = listOf(Language.Ru, Language.Kk)

    /** Все сочетания окна, ступени и языка. */
    val CASES: List<Case> = WINDOWS.flatMap { (width, height) ->
        SCALES.flatMap { scale -> LANGUAGES.map { Case(width, height, scale, it) } }
    }

    /** Одно сочетание: окно, ступень шрифта, язык. */
    data class Case(val width: Int, val height: Int, val scale: TextScale, val language: Language) {
        val look = Look(textScale = scale)
        val tag = "${width}x$height-${scale.code}-${language.name.lowercase()}"
    }

    /** Чек из [count] строк: длинные имена, казахское слово, суммы в миллиарды. */
    fun basket(count: Int): Basket = Basket().apply {
        repeat(count) { at ->
            add(
                Position(
                    name = when (at % 3) {
                        0 -> LONG_NAME
                        1 -> KAZAKH_WORD
                        else -> "Хлеб"
                    },
                    price = if (at % 2 == 0) BILLIONS else BigDecimal("450.00"),
                    quantity = if (at % 2 == 0) BigDecimal.ONE else BigDecimal("1234.567"),
                    vatGroup = "VAT_16",
                    measureUnitCode = "166"
                )
            )
        }
    }

    /** Пять видов оплаты, все принимаются. */
    val PAYMENTS = listOf("CASH", "CARD", "ELECTRONIC", "MOBILE", "CREDIT").map {
        DictionaryEntry(code = it, name = mapOf("ru" to it.lowercase(), "kk" to it.lowercase()))
    }

    /** Форма чека, оплаченного пятью видами сразу. */
    fun fivePayments(): SaleForm = SaleForm().apply {
        PAYMENTS.drop(1).forEach { split.add(it.code) }
        split.entries.filterNot { split.takesRest(it) }.forEach { it.amount = "999999999" }
    }

    /** Документ продажи на миллиарды; отклонённый — с причиной отказа. */
    fun sale(no: Long, refused: Boolean = false) = Document(
        id = "doc-$no",
        docNo = no,
        printedDocumentNumber = no,
        docType = "SALE",
        ofdStatus = if (refused) "FAILED" else "SENT",
        ofdErrorCode = if (refused) REFUSAL else null,
        ofdErrorText = if (refused) "Same customer and taxpayer IIN" else null,
        fiscalSign = "${FISCAL_SIGN + no}",
        totalAmount = BILLIONS_TIYN + no,
        createdAt = System.currentTimeMillis(),
        shiftNo = 7
    )

    /** Состав чека-основания на [count] строк. */
    fun sold(count: Int): List<SoldItem> = (1..count).map {
        SoldItem(
            name = if (it % 2 == 0) LONG_NAME else KAZAKH_WORD,
            price = BILLIONS,
            quantityThousandths = 1000,
            sum = BILLIONS,
            vatGroup = "VAT_16"
        )
    }

    /** Пятьдесят касс с общим началом названия. */
    fun kkms(count: Int = 50): List<Kkm> = (1..count).map {
        Kkm(
            kkmId = "kkm-$it",
            name = "Касса торгового зала магазина «Продукты у дома» на Абая, место $it",
            kkmKgdId = "0000002000${it.toString().padStart(2, '0')}",
            factoryNumber = "SK-0000$it",
            state = "ACTIVE",
            ofdServiceInfo = OrgInfo(orgTitle = "ТОО «Пример»", orgAddress = "Алматы, Абая 150")
        )
    }

    private const val NAME_LENGTH = 200
    private const val REFUSAL = 1015
    private const val FISCAL_SIGN = 3_000_000_000L
    private const val BILLIONS_TIYN = 123_456_789_012L
}
