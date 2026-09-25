package kz.mybrain.superkassa.presentation.cabinet

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import kz.mybrain.superkassa.designsystem.section.AppTopBar
import kz.mybrain.superkassa.designsystem.section.BarLead
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.strings.LocalStrings
import kz.mybrain.superkassa.presentation.cabinet.component.cabinetHead
import kz.mybrain.superkassa.presentation.cabinet.signin.ownerLine
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.strings.api.textsOf

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
 * @param onMenu открыть разделы окна — на телефоне, где их не видно.
 * @param onStep шаг назад по истории окна: открыта карточка точки
 *   или кассы поверх их списка.
 */
@Composable
fun CabinetBar(
    model: CabinetViewModel,
    look: CabinetLook,
    onExit: (() -> Unit)? = null,
    onMenu: (() -> Unit)? = null,
    onStep: (() -> Unit)? = null
) {
    val state by model.state.collectAsScreenState()
    val language = LocalLanguage.current
    val texts = textsOf(language).cabinet
    val journal = textsOf(language).journal.history
    val head = cabinetHead(
        register = state.documentsOf,
        company = state.owner?.company?.name,
        owner = state.owner?.let { ownerLine(it, texts) },
        texts = texts,
        documentsTitle = journal.registerDocuments
    )
    val back = if (head.inDocuments) model::closeDocuments else onStep ?: onExit
    val backLabel = if (head.inDocuments) journal.backToRegister else LocalStrings.current.settingsScreen.back
    AppTopBar(title = head.title, subtitle = head.subtitle, lead = barLead(back, backLabel, onMenu)) {
        CabinetBarActions(look, state.open, texts.signin.signOut, model::signOut)
    }
}

/**
 * Начало шапки кабинета: стрелка назад, если есть куда, кнопка меню
 * на телефоне, иначе ничего — как у всех разделов окна.
 */
@Composable
private fun barLead(back: (() -> Unit)?, backLabel: String, onMenu: (() -> Unit)?): BarLead? = when {
    back != null -> BarLead.Back(back, backLabel)
    onMenu != null -> BarLead.Menu(onMenu, LocalStrings.current.sections.menu)
    else -> null
}

/**
 * Язык и выход — теми же элементами, что и у кассы.
 *
 * Язык переключается и до входа: кабинет государственный, и владелец
 * вправе читать экран входа по-казахски.
 */
@Composable
private fun RowScope.CabinetBarActions(look: CabinetLook, open: Boolean, signOut: String, onSignOut: () -> Unit) {
    look.switches(this)
    if (open) {
        TextButton(onClick = onSignOut) { Text(signOut) }
    }
}
