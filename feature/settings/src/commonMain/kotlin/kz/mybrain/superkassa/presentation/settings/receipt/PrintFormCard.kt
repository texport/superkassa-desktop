package kz.mybrain.superkassa.presentation.settings.receipt

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.picker.SwitchRow
import kz.mybrain.superkassa.designsystem.picker.WideChoiceSegments
import kz.mybrain.superkassa.designsystem.section.PartTitle
import kz.mybrain.superkassa.designsystem.section.SettingGroup
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.presentation.settings.SettingRequirements
import kz.mybrain.superkassa.presentation.settings.title
import kz.mybrain.superkassa.presentation.words.common.of
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Печатная форма чека: язык, ширина ленты и реклама ОФД.
 *
 * Это настройки кассы, а не рабочего места: чек печатается одинаково,
 * с какого бы компьютера его ни пробили. Касса меняет их только в режиме
 * программирования: показывать поля рабочими и отвечать отказом на каждое
 * нажатие значит врать кассиру про её состояние. Почему поля погашены,
 * сказано над ними плашкой требования — как у налогов кассы: погашенный
 * переключатель рекламы без объяснения выглядел сломанным.
 */
@Composable
internal fun PrintFormCard(form: ReceiptFormUiState, actions: ReceiptFormActions) {
    val texts = LocalStrings.current.settingsScreen
    form.kkm ?: return
    val branding = form.branding
    SettingGroup(title = texts.printForm, info = texts.printFormHint) {
        if (!form.editable) SettingRequirements(form.needs, textsOf(LocalLanguage.current).kassa.money.kkm)
        PartTitle(texts.receiptLanguage)
        WideChoiceSegments(
            options = ReceiptLanguageChoice.entries,
            selected = ReceiptLanguageChoice.of(branding.language),
            label = { it.title(texts) },
            enabled = form.editable
        ) { actions.chooseLanguage(it.language) }
        PartTitle(texts.printLayout, texts.printLayoutHint)
        LayoutChoice(form, actions)
        AdsSwitch(form, actions)
        ReceiptLinesSection(form, actions)
    }
}

/** Реклама БФД под итогом чека: строки приходят с ответом на чек. */
@Composable
private fun AdsSwitch(form: ReceiptFormUiState, actions: ReceiptFormActions) {
    val texts = LocalStrings.current.settingsScreen
    SwitchRow(
        title = texts.printOfdAds,
        checked = form.branding.printOfdTicketAds,
        onSwitch = actions::switchOfdAds,
        enabled = form.editable,
        hint = texts.printOfdAdsHint
    )
}

/**
 * Макет печати: узкая лента, широкая или страница.
 *
 * Название своё для знакомых макетов и от кассы для остальных: свои точнее —
 * «Лента 58 мм» вместо «58мм», — но кончаются на первом же макете,
 * которого приложение ещё не знает.
 */
@Composable
private fun LayoutChoice(form: ReceiptFormUiState, actions: ReceiptFormActions) {
    val texts = LocalStrings.current.settingsScreen
    val language = LocalLanguage.current
    WideChoiceSegments(
        options = form.layoutCodes,
        selected = PrintLayout.byMillimetres(form.branding.paperWidthMm).code,
        label = { code ->
            PrintLayout.entries.firstOrNull { it.code == code }?.title(texts)
                ?: form.paperWidths.firstOrNull { it.code == code }?.name?.of(language)
                ?: code
        },
        enabled = form.editable,
        onSelect = actions::chooseLayout
    )
}
