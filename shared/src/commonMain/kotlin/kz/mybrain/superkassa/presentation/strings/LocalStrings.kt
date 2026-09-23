package kz.mybrain.superkassa.presentation.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Надписи текущего языка, доступные любому экрану.
 *
 * Экран не знает, какой язык выбран: он просто берёт строку по смыслу.
 * Так добавление языка не трогает ни один экран.
 */
val LocalStrings: ProvidableCompositionLocal<AppStrings> = staticCompositionLocalOf { KazakhStrings }

/**
 * Язык кассира — для слов, которые касса присылает на трёх языках сразу.
 *
 * Свои надписи экран берёт из [LocalStrings]; язык нужен там, где слова
 * пришли снаружи: отказ кассы, название вида документа из справочника.
 */
val LocalLanguage: ProvidableCompositionLocal<Language> = staticCompositionLocalOf { Language.Kk }

@Composable
fun ProvideStrings(language: Language, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalStrings provides stringsOf(language),
        LocalLanguage provides language,
        content = content
    )
}

/** Надписи выбранного языка. */
fun stringsOf(language: Language): AppStrings = when (language) {
    Language.Kk -> KazakhStrings
    Language.Ru -> RussianStrings
    Language.En -> EnglishStrings
}

/**
 * Название вида оплаты, когда узел его не прислал.
 *
 * В справочнике узла у оплаты в кредит и тарой названий нет — они
 * объявлены устаревшими. Показывать кассиру голый код нельзя: он должен
 * читать чек, а не протокол.
 */
fun EnumStrings.paymentFallback(code: String): String? = when (code) {
    "CASH" -> paymentCash
    "CARD" -> paymentCard
    "ELECTRONIC" -> paymentElectronic
    "MOBILE" -> paymentMobile
    "CREDIT" -> paymentCredit
    "TARE" -> paymentTare
    else -> null
}

/**
 * Название типа документа, когда справочник узла его не знает.
 *
 * В журнале остались записи прежних версий: чеки под общим «CHECK»
 * и X-отчёт под «REPORT_X», которого в справочнике нет. Показывать
 * кассиру код нельзя даже для старой записи.
 */
fun EnumStrings.documentFallback(code: String): String? = when (code) {
    "CHECK", "TICKET", "RECEIPT" -> docCheck
    "REPORT_X", "X_REPORT" -> docReportX
    "SHIFT_OPEN" -> docShiftOpen
    "SHIFT_CLOSE" -> docShiftClose
    "CASH_IN" -> docCashIn
    "CASH_OUT" -> docCashOut
    else -> null
}
