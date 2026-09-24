package kz.mybrain.superkassa.presentation.cabinet

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import kz.mybrain.superkassa.presentation.cabinet.component.cabinetHead
import kz.mybrain.superkassa.presentation.cabinet.signin.ownerLine
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.picker.LanguagePicker
import kz.mybrain.superkassa.presentation.common.picker.ThemeSwitch
import kz.mybrain.superkassa.presentation.common.section.AppTopBar
import kz.mybrain.superkassa.presentation.strings.cabinet.cabinetTexts
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.journal.journalTexts
import kz.mybrain.superkassa.presentation.theme.choice.LookViewModel
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons

/**
 * Шапка кабинета — одна на оба входа в него.
 *
 * В кабинет заходят двумя путями: разделом рабочего места и дверью
 * с экрана входа, до всякого пина кассира. Шапка у них общая: язык
 * и выход не могут быть в одном случае наверху окна, а в другом — нигде.
 *
 * Возврат живёт здесь, а не в экранах под шапкой. Стрелка одна на окно,
 * и ведёт она туда, откуда владелец пришёл: из документов кассы — к её
 * карточке, из кабинета целиком — на экран входа.
 *
 * @param onExit выход из кабинета — только у двери с экрана входа.
 */
@Composable
fun CabinetBar(model: CabinetViewModel, look: LookViewModel, onExit: (() -> Unit)? = null) {
    val state by model.state.collectAsScreenState()
    val language = LocalLanguage.current
    val texts = cabinetTexts(language)
    val journal = journalTexts(language).history
    val head = cabinetHead(
        register = state.documentsOf,
        company = state.owner?.company?.name,
        owner = state.owner?.let { ownerLine(it, texts) },
        texts = texts,
        documentsTitle = journal.registerDocuments
    )
    val back = if (head.inDocuments) model::closeDocuments else onExit
    AppTopBar(
        title = head.title,
        subtitle = head.subtitle,
        badge = AppIcons.cabinet.takeIf { back == null },
        onBack = back,
        backLabel = if (head.inDocuments) journal.backToRegister else LocalStrings.current.settings.back
    ) {
        CabinetBarActions(look, state.open, texts.signOut, model::signOut)
    }
}

/**
 * Язык и выход — теми же элементами, что и у кассы.
 *
 * Язык переключается и до входа: кабинет государственный, и владелец
 * вправе читать экран входа по-казахски.
 */
@Composable
private fun RowScope.CabinetBarActions(look: LookViewModel, open: Boolean, signOut: String, onSignOut: () -> Unit) {
    ThemeSwitch(look)
    LanguagePicker(look)
    if (open) {
        TextButton(onClick = onSignOut) { Text(signOut) }
    }
}
