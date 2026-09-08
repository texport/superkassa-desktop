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
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons

/**
 * Шапка кабинета — одна на оба входа в него.
 *
 * В кабинет заходят двумя путями: разделом рабочего места и дверью
 * с экрана входа, до всякого пина кассира. Шапка у них общая: язык
 * и выход не могут быть в одном случае наверху окна, а в другом — нигде.
 *
 * До входа по ЭЦП шапка называет сам кабинет: компании ещё нет, и выходить
 * не из чего.
 *
 * @param onBack возврат на экран входа — только у двери.
 */
@Composable
fun CabinetBar(session: Session, cabinet: CabinetSession, onBack: (() -> Unit)? = null) {
    val texts = cabinetTexts(session.language)
    AppTopBar(
        title = cabinet.company?.name?.takeIf { it.isNotBlank() } ?: texts.title,
        subtitle = ownerLine(cabinet, texts).takeIf { cabinet.open },
        badge = AppIcons.cabinet,
        onBack = onBack,
        backLabel = LocalStrings.current.settings.back
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
    LanguagePicker(session)
    if (cabinet.open) {
        TextButton(onClick = { scope.launch { cabinet.signOut() } }) {
            Text(cabinetTexts(session.language).signOut)
        }
    }
}
