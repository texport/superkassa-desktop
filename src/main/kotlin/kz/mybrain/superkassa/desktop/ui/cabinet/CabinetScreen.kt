package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.cabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Личный кабинет ОФД внутри рабочего места.
 *
 * Здесь работает владелец, а не кассир: компания и виды деятельности,
 * торговые точки, кассы с постановкой на учёт через ИСНА и всё, что
 * доехало до ОФД. Поэтому и вход свой — по ЭЦП владельца, а не по пину
 * кассира, и живёт он только пока приложение открыто.
 *
 * Пока владелец не вошёл, раздел показывает одну кнопку входа: показывать
 * пустые списки компании, которой ещё нет, значит обещать данные, которых
 * взять неоткуда.
 */
@Composable
fun CabinetScreen(session: Session, cabinet: CabinetSession) {
    val texts = cabinetTexts(session.language)
    if (!cabinet.open) {
        CabinetSignIn(session, cabinet, texts)
        return
    }
    var page by remember { mutableStateOf(CabinetTab.Company) }
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        CabinetTabs(page, texts) { page = it }
        CabinetPage(session, cabinet, texts, page)
    }
}

/**
 * Разделы кабинета — вкладками, а не сегментами.
 *
 * Сегменты Material 3 отвечают за выбор значения — вид документа, причину
 * снятия с учёта, — и такие переключатели в кабинете тоже есть. Переход
 * между разделами рабочего места — другой случай, и гайдлайн отводит ему
 * вкладки: у них есть подчёркивание текущего раздела и черта, отделяющая
 * шапку от содержимого. Четырьмя сегментами подряд разделы читались как
 * ещё один фильтр над списком.
 */
@Composable
private fun CabinetTabs(page: CabinetTab, texts: CabinetTexts, onSelect: (CabinetTab) -> Unit) {
    PrimaryTabRow(selectedTabIndex = page.ordinal, containerColor = Color.Transparent) {
        CabinetTab.entries.forEach { tab ->
            Tab(
                selected = tab == page,
                onClick = { onSelect(tab) },
                text = { Text(text = tab.title(texts), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
    }
}

/** Содержимое выбранного раздела. */
@Composable
private fun CabinetPage(
    session: Session,
    cabinet: CabinetSession,
    texts: CabinetTexts,
    page: CabinetTab
) {
    when (page) {
        CabinetTab.Company -> CompanyPage(cabinet, texts, session.language)
        CabinetTab.Places -> PlacesPage(session, cabinet, texts)
        CabinetTab.Registers -> RegistersPage(session, cabinet, texts)
        CabinetTab.Documents -> DocumentsPage(cabinet, texts)
    }
}

/** Разделы кабинета: тот же порядок, в каком владелец их заводит. */
enum class CabinetTab(val title: (CabinetTexts) -> String) {
    Company({ it.company }),
    Places({ it.places }),
    Registers({ it.registers }),
    Documents({ it.documents })
}
