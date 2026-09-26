package kz.mybrain.superkassa.presentation.analytics.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.adaptive.WrapRow
import kz.mybrain.superkassa.designsystem.field.SearchField
import kz.mybrain.superkassa.designsystem.picker.MenuChip
import kz.mybrain.superkassa.designsystem.section.CounterTile
import kz.mybrain.superkassa.designsystem.section.SectionTitle
import kz.mybrain.superkassa.designsystem.state.ScreenSlot
import kz.mybrain.superkassa.designsystem.state.ScreenState
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.designsystem.tip.InfoTip
import kz.mybrain.superkassa.domain.analytics.model.ExchangeAddress
import kz.mybrain.superkassa.domain.analytics.model.exchangeAddressCount
import kz.mybrain.superkassa.domain.analytics.model.exchangeRegisters
import kz.mybrain.superkassa.presentation.analytics.common.analyticsScreenState
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.strings.api.analytics.AnalyticsTexts

/**
 * Адреса, с которых кассы выходили на связь.
 *
 * Список спрашивается целиком по компании, а отбор по кассе и поиск
 * работают уже по нему: ждать сеть на каждое нажатие в отборе незачем.
 *
 * Сведение служебное, и потому оно только на экране: в журнал
 * приложения адреса обмена не пишутся.
 */
@Composable
internal fun AnalyticsExchangeScreen(model: AnalyticsExchangeViewModel, access: String?, texts: AnalyticsTexts) {
    val state by model.state.collectAsScreenState()
    LaunchedEffect(access) { model.follow(access) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
    ) {
        ExchangeHead(state, texts, model::refresh)
        ExchangeFilters(state, texts, model::search, model::pick)
        ScreenSlot(exchangeState(state, texts, model::refresh), Modifier.weight(1f)) {
            AnalyticsExchangeList(state.rows, texts, Modifier.weight(1f))
        }
    }
}

/**
 * Заголовок раздела, счётчики и обновление.
 *
 * Счётчики считаются по самому списку: кабинет присылает число записей
 * об обмене, а не число разных адресов, и над таблицей с одним адресом
 * стояло «адресов: 4».
 */
@Composable
private fun ExchangeHead(state: AnalyticsExchangeUiState, texts: AnalyticsTexts, onRefresh: () -> Unit) {
    val all = state.all
    WrapRow(modifier = Modifier.fillMaxWidth(), spacing = Spacing.cardGap) {
        SectionTitle(texts.exchangeTitle)
        InfoTip(texts.exchangeHint)
        Spacer(Modifier.weight(1f))
        CounterTile(Money.count(exchangeRegisters(all).size), texts.kkmCount)
        CounterTile(Money.count(exchangeAddressCount(all)), texts.addressCount)
        IconButton(onClick = onRefresh, enabled = !state.reading.loading) {
            Icon(AppIcons.refresh, contentDescription = texts.refresh)
        }
    }
}

/**
 * Поиск и отбор по кассе.
 *
 * Чем этот раздел является по существу, объяснено под значком у его
 * заголовка: читают такое один раз, а абзац под заголовком занимал
 * место у самих адресов.
 *
 * Поле поиска — общее для приложения: со значком и кнопкой очистки,
 * как в отборе карты. Голое поле без значка не читалось как поиск,
 * а забытое в нём слово выглядело как пропавшие адреса.
 *
 * Поле тянется во всю ширину ряда до плашки отбора: шириной по подписи
 * оно было уже своей подсказки и при нажатии раздавалось, сдвигая плашку.
 */
@Composable
private fun ExchangeFilters(
    state: AnalyticsExchangeUiState,
    texts: AnalyticsTexts,
    onSearch: (String) -> Unit,
    onPick: (String?) -> Unit
) {
    val registers = exchangeRegisters(state.all)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchField(
            value = state.query,
            label = texts.searchLabel,
            onChange = onSearch,
            modifier = Modifier.weight(1f),
            // Подпись короткая, а чем искать — примером в самом поле, как
            // в отборе карты: длинная подпись в поле переносилась на две строки.
            hint = texts.search,
            clearLabel = texts.sieve.clear
        )
        RegisterChip(state.register, registers, texts, onPick)
    }
}

/** Отбор по кассе — плашкой со списком касс, у которых есть адреса обмена. */
@Composable
private fun RegisterChip(
    chosen: String?,
    registers: List<ExchangeAddress>,
    texts: AnalyticsTexts,
    onPick: (String?) -> Unit
) {
    val titles = remember(registers) { registers.associate { it.cashRegisterId to it.title } }
    MenuChip(
        value = registerTitle(chosen, titles, texts),
        options = listOf<String?>(null) + registers.map { it.cashRegisterId },
        title = { registerTitle(it, titles, texts) },
        chosen = chosen != null,
        onSelect = onPick
    )
}

/** Помеха, ожидание, пустота или сам список. */
private fun exchangeState(state: AnalyticsExchangeUiState, texts: AnalyticsTexts, onRetry: () -> Unit): ScreenState =
    analyticsScreenState(state.reading, texts, onRetry) {
        when {
            state.all.isEmpty() -> ScreenState.Empty(AppIcons.noDocuments, texts.exchangeEmpty, texts.exchangeEmptyHint)
            state.rows.isEmpty() -> ScreenState.Empty(AppIcons.find, texts.exchangeNotFound, texts.exchangeNotFoundHint)
            else -> null
        }
    }

/**
 * Как названа касса в отборе; `null` — все кассы.
 *
 * Названия берутся из готового словаря: поиск по списку для каждой строки
 * открытого меню делал его квадратичным по числу касс.
 */
private fun registerTitle(id: String?, titles: Map<String, String>, texts: AnalyticsTexts): String =
    id?.let { titles[it] } ?: texts.allRegisters
