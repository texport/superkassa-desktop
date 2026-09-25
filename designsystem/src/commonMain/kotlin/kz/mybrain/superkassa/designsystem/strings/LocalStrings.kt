package kz.mybrain.superkassa.designsystem.strings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.common.CommonTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Надписи текущего языка, доступные любому экрану.
 *
 * Экран не знает, какой язык выбран: он просто берёт строку по смыслу.
 * Так добавление языка не трогает ни один экран.
 */
val LocalStrings: ProvidableCompositionLocal<CommonTexts> = staticCompositionLocalOf { textsOf(Language.Kk).common }

/**
 * Язык кассира — для слов, которые касса присылает на трёх языках сразу.
 *
 * Свои надписи экран берёт из [LocalStrings]; язык нужен там, где слова
 * пришли снаружи: отказ кассы, название вида документа из справочника.
 */
val LocalLanguage: ProvidableCompositionLocal<Language> = staticCompositionLocalOf { Language.Kk }

/**
 * Надписи и язык [language] для всего, что внутри [content].
 *
 * Тексты берутся у модуля текстов; здесь только их привязка к дереву
 * Compose.
 */
@Composable
fun ProvideStrings(language: Language, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalStrings provides textsOf(language).common,
        LocalLanguage provides language,
        content = content
    )
}
