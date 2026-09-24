package kz.mybrain.superkassa.presentation.settings.receipt

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.presentation.common.picker.SwitchRow
import kz.mybrain.superkassa.presentation.common.picker.WideChoiceSegments
import kz.mybrain.superkassa.presentation.common.section.PartTitle
import kz.mybrain.superkassa.presentation.common.section.SectionCard
import kz.mybrain.superkassa.presentation.settings.title
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.common.of

/**
 * Печатная форма чека: язык, ширина ленты и реклама ОФД.
 *
 * Это настройки кассы, а не рабочего места: чек печатается одинаково,
 * с какого бы компьютера его ни пробили. Касса меняет их только в режиме
 * программирования: показывать поля рабочими и отвечать отказом на каждое
 * нажатие значит врать кассиру про её состояние.
 */
@Composable
fun PrintFormCard(form: ReceiptFormUiState, actions: ReceiptFormActions) {
    val texts = LocalStrings.current.settings
    form.kkm ?: return
    val branding = form.branding
    SectionCard(title = texts.printForm, info = texts.printFormHint) {
        PartTitle(texts.receiptLanguage)
        WideChoiceSegments(
            options = ReceiptLanguageChoice.entries,
            selected = ReceiptLanguageChoice.of(branding.language),
            label = { it.title(texts) },
            enabled = form.editable
        ) { actions.chooseLanguage(it.language) }
        PartTitle(texts.printLayout, texts.printLayoutHint)
        LayoutChoice(form, actions)
        SwitchRow(
            title = texts.printOfdAds,
            checked = branding.printOfdTicketAds,
            onSwitch = actions::switchOfdAds,
            enabled = form.editable,
            hint = texts.printOfdAdsHint
        )
        ReceiptLinesSection(form, actions)
    }
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
    val texts = LocalStrings.current.settings
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
