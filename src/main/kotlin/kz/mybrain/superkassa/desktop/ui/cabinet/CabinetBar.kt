package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.components.AppTopBar
import kz.mybrain.superkassa.desktop.ui.components.LanguagePicker
import kz.mybrain.superkassa.desktop.ui.components.ThemeSwitch
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.journalTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons

/**
 * Шапка кабинета — одна на оба входа в него.
 *
 * В кабинет заходят двумя путями: разделом рабочего места и дверью
 * с экрана входа, до всякого пина кассира. Шапка у них общая: язык
 * и выход не могут быть в одном случае наверху окна, а в другом — нигде.
 *
 * Возврат живёт здесь, а не в экранах под шапкой. Стрелка одна на окно,
 * и ведёт она туда, откуда владелец пришёл: из документов кассы — к её
 * карточке, из кабинета целиком — на экран входа. Прежде документы
 * рисовали свою стрелку внутри себя, и она уезжала вместе с содержимым.
 *
 * @param onExit выход из кабинета — только у двери с экрана входа.
 */
@Composable
fun CabinetBar(
    session: Session,
    cabinet: CabinetSession,
    documents: CabinetDocuments,
    onExit: (() -> Unit)? = null
) {
    val texts = cabinetTexts(session.language)
    val journal = journalTexts(session.language).history
    val head = cabinetHead(
        register = documents.register,
        company = cabinet.company?.name,
        owner = ownerLine(cabinet, texts).takeIf { cabinet.open },
        texts = texts,
        documentsTitle = journal.registerDocuments
    )
    val back = if (head.inDocuments) ({ documents.register = null }) else onExit
    AppTopBar(
        title = head.title,
        subtitle = head.subtitle,
        badge = AppIcons.cabinet.takeIf { back == null },
        onBack = back,
        backLabel = if (head.inDocuments) journal.backToRegister else LocalStrings.current.settings.back
    ) {
        CabinetBarActions(session, cabinet)
    }
}

/**
 * Язык и выход — теми же элементами, что и у кассы.
 *
 * Язык переключается и до входа: кабинет государственный, и владелец
 * вправе читать экран входа по-казахски.
 */
@Composable
private fun RowScope.CabinetBarActions(session: Session, cabinet: CabinetSession) {
    val scope = rememberCoroutineScope()
    ThemeSwitch(session)
    LanguagePicker(session)
    if (cabinet.open) {
        TextButton(onClick = { scope.launch { cabinet.signOut() } }) {
            Text(cabinetTexts(session.language).signOut)
        }
    }
}
