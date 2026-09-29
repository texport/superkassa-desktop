package kz.mybrain.superkassa.presentation.cabinet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.cabinet.model.CabinetRegister
import kz.mybrain.superkassa.navigation.LocalNavigator
import kz.mybrain.superkassa.navigation.step.PlaceCardKey
import kz.mybrain.superkassa.navigation.step.RegisterDocumentsKey
import kz.mybrain.superkassa.navigation.step.StepKey
import kz.mybrain.superkassa.presentation.cabinet.company.CompanyScreen
import kz.mybrain.superkassa.presentation.cabinet.documents.CabinetDocumentsScreen
import kz.mybrain.superkassa.presentation.cabinet.documents.LocalRegisterDocuments
import kz.mybrain.superkassa.presentation.cabinet.places.PlacesScreen
import kz.mybrain.superkassa.presentation.cabinet.signin.CabinetSignIn
import kz.mybrain.superkassa.presentation.cabinet.signin.actions
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts
import kz.mybrain.superkassa.strings.api.textsOf

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
 *
 * @param step открытый шаг истории окна: карточка точки или кассы поверх
 *   их списка на узком окне, документы кассы поверх её карточки. Вкладки —
 *   уровень списка, и в шаге их нет.
 */
@Composable
fun CabinetScreen(window: CabinetWindow, step: StepKey? = null) {
    val model = window.cabinet
    val state by model.state.collectAsScreenState()
    val language = LocalLanguage.current
    val texts = textsOf(language).cabinet
    if (!state.open) {
        CabinetSignIn(state, language, texts, model.actions(), model.signature)
        return
    }
    // Вошли из мастера — хозяйство не читалось: раздел читает его сам.
    // Прочитанное раньше обновляется в кассах этой машины: их меняют и вне
    // кабинета, а весь список сети — сотни обращений.
    LaunchedEffect(state.owner) { if (!state.placesRead) model.reload() else model.rereadHere() }
    when (step) {
        RegisterDocumentsKey -> DocumentsStep(model, texts, state.documentsOf)
        else -> CabinetLevels(window, texts, stepped = step is PlaceCardKey)
    }
}

/**
 * Список и карточки кабинета. Документы кассы открываются шагом истории
 * окна поверх её карточки: «назад» — жест, Escape, стрелка в шапке —
 * возвращает к той же карточке, а выбранные точка и касса остаются
 * выбранными.
 */
@Composable
private fun CabinetLevels(window: CabinetWindow, texts: CabinetTexts, stepped: Boolean) {
    val model = window.cabinet
    val navigator = LocalNavigator.current
    val open: (CabinetRegister) -> Unit = { register ->
        model.view.openDocuments(register)
        navigator.open(RegisterDocumentsKey)
    }
    CompositionLocalProvider(LocalRegisterDocuments provides open) {
        if (stepped) PlacesScreen(window, texts, stepped = true) else CabinetTabsBody(window, texts)
    }
}

/**
 * Документы кассы шагом истории окна.
 *
 * Шаг снят — документы закрыты: как бы владелец ни ушёл, шапка перестаёт
 * называть их. Шаг без кассы — модель выгружена вместе с приложением —
 * снимается сам: пустой экран вместо карточки ничего не даёт.
 */
@Composable
private fun DocumentsStep(model: CabinetViewModel, texts: CabinetTexts, register: CabinetRegister?) {
    val navigator = LocalNavigator.current
    LaunchedEffect(register == null) { if (register == null) navigator.back() }
    DisposableEffect(model) { onDispose(model.view::closeDocuments) }
    if (register != null) CabinetDocumentsScreen(model, texts, register)
}

/**
 * Вкладки и раздел под ними.
 *
 * Вкладки стоят вплотную под шапкой окна, как по Material 3: поле над ними
 * отнимало у списка точек в малом окне ещё четверть строки и ничего
 * не отделяло — вкладки и так черта.
 *
 * Открытая вкладка живёт в модели кабинета: вернувшись из карточки точки,
 * из документов кассы или из другого раздела окна, владелец видит ту
 * вкладку, из которой уходил, а не вкладку компании.
 */
@Composable
private fun CabinetTabsBody(window: CabinetWindow, texts: CabinetTexts) {
    val page by window.cabinet.view.tab.collectAsScreenState()
    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
    ) {
        CabinetTabs(page, LocalLanguage.current) { window.cabinet.view.selectTab(it) }
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
        CabinetTab.Analytics -> window.neighbours.analytics(access, texts)
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
internal enum class CabinetTab(val title: (Language) -> String) {
    Company({ textsOf(it).cabinet.company.title }),
    Places({ textsOf(it).cabinet.places.title }),
    Analytics({ textsOf(it).analytics.title })
}
