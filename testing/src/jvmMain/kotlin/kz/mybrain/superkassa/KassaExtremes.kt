package kz.mybrain.superkassa

import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.PaymentTypeResponse
import io.github.texport.superkassa.core.presentation.api.model.reference.TrilingualMessageResponse
import kz.mybrain.superkassa.designsystem.theme.Look
import kz.mybrain.superkassa.designsystem.theme.TextScale
import kz.mybrain.superkassa.domain.kassa.model.decimal
import kz.mybrain.superkassa.domain.kassa.model.payment.PaymentSplit
import kz.mybrain.superkassa.domain.kassa.model.sale.Basket
import kz.mybrain.superkassa.domain.kassa.model.sale.Position
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleForm
import kz.mybrain.superkassa.domain.kassa.model.tenge
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.strings.api.Language
import java.math.BigDecimal

/**
 * Предельные данные кассовых экранов.
 *
 * Разметку ломает не обычный чек, а крайний: наименование классификатора
 * в двести знаков, казахское слово без пробелов, сумма в миллиарды, чек
 * на тысячу строк. Всё это собрано здесь один раз, чтобы экраны проверялись
 * на одном и том же.
 */
object KassaExtremes {

    /** Наименование на двести знаков — формулировка классификатора целиком. */
    val LONG_NAME = (
        "Говядина охлаждённая высшего сорта, вырезка без кости, фасованная в вакуумную упаковку " +
            "по технологии длительного хранения, для ресторанов и магазинов сети, партия номер 000417"
        ).repeat(2).take(NAME_LENGTH)

    /** Длинное казахское слово без единого пробела. */
    const val KAZAKH_WORD = "Мемлекеттікбағдарламаныңорындалуынқамтамасызетушілердің"

    /** Сумма в миллиарды тенге, в тиынах. */
    val BILLIONS: Long = tenge("1234567890.12")

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
    fun basket(count: Int): Basket = (0 until count).fold(Basket()) { basket, at ->
        basket.add(
            Position(
                name = when (at % 3) {
                    0 -> LONG_NAME
                    1 -> KAZAKH_WORD
                    else -> "Хлеб"
                },
                price = if (at % 2 == 0) BILLIONS else tenge("450.00"),
                quantity = if (at % 2 == 0) decimal("1") else decimal("1234.567"),
                vatGroup = "VAT_16",
                measureUnitCode = "116"
            )
        )
    }

    /** Пять видов оплаты, все принимаются. */
    val PAYMENTS = listOf("CASH", "CARD", "ELECTRONIC", "MOBILE", "CREDIT").map {
        PaymentTypeResponse(
            it,
            TrilingualMessageResponse(it.lowercase(), it.lowercase(), it.lowercase()),
            supported = true
        )
    }

    /** Форма чека, оплаченного пятью видами сразу. */
    fun fivePayments(): SaleForm {
        val split = PAYMENTS.drop(1).fold(PaymentSplit()) { split, type -> split.add(type.code) }
        return SaleForm(split = split.entries.indices.fold(split) { all, at -> all.enter(at, "999999999") })
    }

    /** Документ продажи на миллиарды; отклонённый — с причиной отказа. */
    fun sale(no: Long, refused: Boolean = false) =
        CoreScene.document("doc-$no", amount = BILLIONS_TIYN + no, status = if (refused) "FAILED" else "SENT").copy(
            docNo = no,
            printedDocumentNumber = no,
            ofdErrorCode = if (refused) REFUSAL else null,
            ofdErrorText = if (refused) "Same customer and taxpayer IIN" else null,
            fiscalSign = "${FISCAL_SIGN + no}",
            createdAt = System.currentTimeMillis()
        )

    /** Строка чека-основания: цена и сумма в тенге строкой, количество — в тысячных. */
    data class SoldLine(val name: String, val price: String, val quantityThousandths: Long, val sum: String)

    /** Состав чека-основания на [count] строк. */
    fun sold(count: Int): List<SoldLine> = (1..count).map {
        val money = BigDecimal.valueOf(BILLIONS, 2).toPlainString()
        SoldLine(
            name = if (it % 2 == 0) LONG_NAME else KAZAKH_WORD,
            price = money,
            quantityThousandths = 1000,
            sum = money
        )
    }

    /** Пятьдесят касс с общим началом названия. */
    fun kkms(count: Int = 50): List<KkmResponse> = (1..count).map {
        CoreScene.kkm(
            id = "kkm-$it",
            name = "Касса торгового зала магазина «Продукты у дома» на Абая, место $it",
            kgd = "0000002000${it.toString().padStart(2, '0')}"
        ).copy(factoryNumber = "SK-0000$it")
    }

    private const val NAME_LENGTH = 200
    private const val REFUSAL = 1015
    private const val FISCAL_SIGN = 3_000_000_000L
    private const val BILLIONS_TIYN = 123_456_789_012L
}
