package kz.mybrain.superkassa.presentation.cabinet

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
import kz.mybrain.superkassa.presentation.analytics.AnalyticsScreen
import kz.mybrain.superkassa.presentation.cabinet.company.CompanyScreen
import kz.mybrain.superkassa.presentation.cabinet.documents.CabinetDocumentsScreen
import kz.mybrain.superkassa.presentation.cabinet.documents.LocalRegisterDocuments
import kz.mybrain.superkassa.presentation.cabinet.places.PlacesScreen
import kz.mybrain.superkassa.presentation.cabinet.signin.CabinetSignIn
import kz.mybrain.superkassa.presentation.cabinet.signin.actions
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.strings.analytics.analyticsTexts
import kz.mybrain.superkassa.presentation.strings.cabinet.CabinetTexts
import kz.mybrain.superkassa.presentation.strings.cabinet.cabinetTexts
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.theme.size.Spacing

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
fun CabinetScreen(window: CabinetWindow) {
    val model = window.cabinet
    val state by model.state.collectAsScreenState()
    val language = LocalLanguage.current
    val texts = cabinetTexts(language)
    if (!state.open) {
        CabinetSignIn(state, language, texts, model.actions())
        return
    }
    Box(modifier = Modifier.fillMaxSize()) {
        // Документы кассы открываются экраном поверх кабинета, а не вместо
        // него: выбранная точка и выбранная касса остаются выбранными, и по
        // возврату владелец видит ту же карточку, из которой уходил. Возврат
        // рисует шапка окна: навигация в приложении одна и живёт там.
        CompositionLocalProvider(LocalRegisterDocuments provides model::openDocuments) {
            CabinetTabsBody(window, texts)
        }
        val register = state.documentsOf
        if (register != null) {
            Surface(modifier = Modifier.fillMaxSize()) {
                CabinetDocumentsScreen(model, texts, register)
            }
        }
    }
}

/**
 * Вкладки и раздел под ними.
 *
 * Вкладки стоят вплотную под шапкой окна, как по Material 3: поле над ними
 * отнимало у списка точек в малом окне ещё четверть строки и ничего
 * не отделяло — вкладки и так черта.
 */
@Composable
private fun CabinetTabsBody(window: CabinetWindow, texts: CabinetTexts) {
    var page by remember { mutableStateOf(CabinetTab.Company) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = Spacing.screen, end = Spacing.screen, bottom = Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        CabinetTabs(page, LocalLanguage.current) { page = it }
        CabinetPage(window, texts, page)
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
private fun CabinetPage(window: CabinetWindow, texts: CabinetTexts, page: CabinetTab) {
    val access = window.cabinet.state.collectAsScreenState().value.access
    when (page) {
        CabinetTab.Company -> CompanyScreen(window.cabinet, LocalLanguage.current, texts)
        CabinetTab.Places -> PlacesScreen(window, texts)
        CabinetTab.Analytics -> AnalyticsScreen(window.app, access, texts)
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
