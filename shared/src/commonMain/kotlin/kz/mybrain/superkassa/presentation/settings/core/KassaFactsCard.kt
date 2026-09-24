package kz.mybrain.superkassa.presentation.settings.core

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.presentation.common.section.FactLines
import kz.mybrain.superkassa.presentation.common.section.SectionCard
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.kassa.moneyTexts
import kz.mybrain.superkassa.presentation.strings.settings.coreSettingTexts
import kz.mybrain.superkassa.presentation.strings.settings.kassaFactsTexts

/**
 * Сведения о кассе на этой машине: версии, режим, протокол, хранилище,
 * каталог данных и число касс.
 *
 * Первое, что спрашивает поддержка при разборе. Читаются сами при открытии
 * настроек и видны без входа: нужны они как раз тогда, когда касса
 * не открылась или не пускает, а сменить здесь нечего — и секретов нет.
 */
@Composable
internal fun KassaFactsCard(core: CoreSettingsUiState) {
    val language = LocalLanguage.current
    val texts = kassaFactsTexts(language)
    SectionCard(title = texts.title, info = texts.hint) {
        FactLines(null, core.facts(texts, coreSettingTexts(language), moneyTexts(language).kkm), texts.unread)
    }
}
