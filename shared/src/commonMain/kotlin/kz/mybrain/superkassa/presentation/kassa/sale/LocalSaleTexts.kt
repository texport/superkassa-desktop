package kz.mybrain.superkassa.presentation.kassa.sale

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import kz.mybrain.superkassa.presentation.strings.kassa.SaleTexts
import kz.mybrain.superkassa.presentation.strings.kassa.saleTextsKk

/**
 * Надписи области продажи на языке кассира.
 *
 * Экран кладёт их в контекст один раз, и части экрана берут строку
 * по смыслу, не зная выбранного языка.
 */
val LocalSaleTexts: ProvidableCompositionLocal<SaleTexts> = staticCompositionLocalOf { saleTextsKk }
