package kz.mybrain.superkassa.presentation.kassa.sale

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.kassa.SaleTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Надписи области продажи на языке кассира.
 *
 * Экран кладёт их в контекст один раз, и части экрана берут строку
 * по смыслу, не зная выбранного языка.
 */
internal val LocalSaleTexts: ProvidableCompositionLocal<SaleTexts> =
    staticCompositionLocalOf { textsOf(Language.Kk).kassa.sale }
