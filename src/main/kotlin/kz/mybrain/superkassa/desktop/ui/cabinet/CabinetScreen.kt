package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.CabinetRegister
import kz.mybrain.superkassa.desktop.ui.analytics.AnalyticsPage
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.analyticsTexts
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
fun CabinetScreen(
    session: Session,
    cabinet: CabinetSession,
    // Документы кассы открываются экраном поверх кабинета, а не вместо
    // него: выбранная точка и выбранная касса остаются выбранными, и по
    // возврату владелец видит ту же карточку, из которой уходил. Состояние
    // приходит снаружи: возврат из документов рисует шапка окна.
    documents: CabinetDocuments
) {
    val texts = cabinetTexts(session.language)
    if (!cabinet.open) {
        CabinetSignIn(session, cabinet, texts)
        return
    }
    var page by remember { mutableStateOf(CabinetTab.Company) }
    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalRegisterDocuments provides { documents.register = it }) {
            Column(
                modifier = Modifier.fillMaxSize().padding(Spacing.screen),
                verticalArrangement = Arrangement.spacedBy(Spacing.snug)
            ) {
                CabinetTabs(page, session.language) { page = it }
                CabinetPage(session, cabinet, texts, page)
            }
        }
        val register = documents.register
        if (register != null) {
            Surface(modifier = Modifier.fillMaxSize()) {
                CabinetDocumentsScreen(session, cabinet, texts, register)
            }
        }
    }
}

/**
 * Чьи документы открыты поверх кабинета.
 *
 * Состояние вынесено из экрана наружу, потому что о нём нужно знать шапке
 * окна: навигация в приложении одна и живёт там. Пока документы открыты,
 * стрелка шапки уводит к карточке кассы, а не из кабинета целиком.
 */
class CabinetDocuments {
    var register: CabinetRegister? by mutableStateOf(null)
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
private fun CabinetTabs(page: CabinetTab, language: Language, onSelect: (CabinetTab) -> Unit) {
    PrimaryTabRow(selectedTabIndex = page.ordinal, containerColor = Color.Transparent) {
        CabinetTab.entries.forEach { tab ->
            Tab(
                selected = tab == page,
                onClick = { onSelect(tab) },
                text = { Text(text = tab.title(language), maxLines = 1, overflow = TextOverflow.Ellipsis) }
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
        CabinetTab.Company -> CompanyPage(session, cabinet, texts)
        CabinetTab.Places -> PlacesPage(session, cabinet, texts)
        CabinetTab.Analytics -> AnalyticsPage(session, cabinet, texts)
    }
}

/**
 * Разделы кабинета.
 *
 * Кассы и документы своих разделов не имеют: касса стоит в торговой
 * точке, документы принадлежат кассе, и разложенные по отдельным
 * вкладкам они заставляли владельца выбирать одно и то же дважды —
 * точку в одной вкладке, ту же кассу в другой, её же в третьей.
 *
 * Аналитика — раздел сам по себе: она смотрит на всё хозяйство разом,
 * а не на выбранную кассу, и выбирать в ней нечего.
 *
 * Название раздела берётся по языку, а не из готового набора кабинета:
 * у аналитики набор надписей свой.
 */
enum class CabinetTab(val title: (Language) -> String) {
    Company({ cabinetTexts(it).company }),
    Places({ cabinetTexts(it).places }),
    Analytics({ analyticsTexts(it).title })
}
