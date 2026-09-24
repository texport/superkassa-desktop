package kz.mybrain.superkassa.presentation.words.kassa

import kz.mybrain.superkassa.designsystem.theme.icon.Glyphs
import kz.mybrain.superkassa.domain.kassa.model.entry.DraftProblem
import kz.mybrain.superkassa.domain.kassa.model.entry.LookupProblem
import kz.mybrain.superkassa.domain.kassa.model.sale.AdjustmentUnit
import kz.mybrain.superkassa.domain.kassa.model.sale.ExciseRefusal
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.common.SaleStrings
import kz.mybrain.superkassa.strings.api.kassa.SaleTexts
import kz.mybrain.superkassa.strings.api.textsOf

/*
 * Слова ввода позиции: чем негодна набранная позиция, считанная марка
 * и поиск по коду. Правило называет беду значением, слова — здесь.
 */

/** Что не так с набранной позицией — словами под полем. */
fun DraftProblem.text(texts: SaleTexts): String = when (this) {
    DraftProblem.NameMissing -> texts.needName
    DraftProblem.PriceNotANumber, DraftProblem.QuantityNotANumber, DraftProblem.DiscountNotANumber -> texts.notANumber
    DraftProblem.PriceTooPrecise, DraftProblem.DiscountTooPrecise -> texts.pricePrecision
    DraftProblem.PriceNotPositive -> texts.needPrice
    DraftProblem.QuantityTooPrecise -> texts.quantityPrecision
    DraftProblem.QuantityNotPositive -> texts.needQuantity
    DraftProblem.QuantityNotWhole -> texts.quantityWhole
    DraftProblem.DiscountTooBig -> texts.discountTooBig
    DraftProblem.DiscountNegative -> texts.discountNegative
    DraftProblem.DiscountOverPercent -> texts.blockPercentRange
}

/**
 * Знак способа ввода скидки — из общего набора: своя копия тенге однажды
 * уже разошлась с той, которой подписаны суммы.
 */
val AdjustmentUnit.sign: String
    get() = when (this) {
        AdjustmentUnit.Tenge -> Glyphs.TENGE
        AdjustmentUnit.Percent -> Glyphs.PERCENT
    }

/** Почему марка не встала в перечень. */
fun ExciseRefusal.words(texts: SaleTexts): String = when (this) {
    ExciseRefusal.TooLong -> texts.exciseTooLong
    ExciseRefusal.Repeated -> texts.exciseRepeated
}

/**
 * Беда поиска по коду словами кассира и с тем, что делать сейчас.
 *
 * Причину блокировки называет общее место — то же, откуда её берут
 * главный экран и смена: своя фраза здесь развела бы одну блокировку
 * на два разных объяснения.
 */
fun lookupProblemWords(problem: LookupProblem, blockReason: Int?, language: Language, texts: SaleStrings): String =
    when (problem) {
        LookupProblem.Missing -> texts.barcodeMissing
        LookupProblem.Unavailable -> texts.barcodeUnavailable
        LookupProblem.Blocked -> textsOf(language).kassa.blockReason.words(blockReason)
    }
