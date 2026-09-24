package kz.mybrain.superkassa.presentation.analytics.exchange

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.domain.analytics.model.ExchangeAddress
import kz.mybrain.superkassa.domain.analytics.model.exchangeAddressCount
import kz.mybrain.superkassa.domain.analytics.model.exchangeRegisters
import kz.mybrain.superkassa.presentation.analytics.common.analyticsScreenState
import kz.mybrain.superkassa.presentation.common.adaptive.WrapRow
import kz.mybrain.superkassa.presentation.common.field.SearchField
import kz.mybrain.superkassa.presentation.common.field.fieldWidth
import kz.mybrain.superkassa.presentation.common.format.Money
import kz.mybrain.superkassa.presentation.common.message.InfoTip
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.picker.MenuChip
import kz.mybrain.superkassa.presentation.common.section.CounterTile
import kz.mybrain.superkassa.presentation.common.section.SectionTitle
import kz.mybrain.superkassa.presentation.common.state.ScreenSlot
import kz.mybrain.superkassa.presentation.common.state.ScreenState
import kz.mybrain.superkassa.presentation.strings.analytics.AnalyticsTexts
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import kz.mybrain.superkassa.presentation.theme.size.Spacing

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
fun AnalyticsExchangeScreen(model: AnalyticsExchangeViewModel, access: String?, texts: AnalyticsTexts) {
    val state by model.state.collectAsScreenState()
    LaunchedEffect(access) { model.follow(access) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
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
    WrapRow(modifier = Modifier.fillMaxWidth(), spacing = Spacing.normal) {
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
 */
@Composable
private fun ExchangeFilters(
    state: AnalyticsExchangeUiState,
    texts: AnalyticsTexts,
    onSearch: (String) -> Unit,
    onPick: (String?) -> Unit
) {
    val registers = exchangeRegisters(state.all)
    WrapRow(modifier = Modifier.fillMaxWidth(), spacing = Spacing.snug) {
        SearchField(
            value = state.query,
            label = texts.searchLabel,
            onChange = onSearch,
            modifier = Modifier.fieldWidth(texts.searchLabel, Sizes.fieldSearch),
            // Подпись короткая, а чем искать — примером в самом поле, как
            // в отборе карты: длинная подпись в поле переносилась на две строки.
            hint = texts.search,
            clearLabel = texts.sieve.clear
        )
        MenuChip(
            value = registerTitle(state.register, registers, texts),
            options = listOf<String?>(null) + registers.map { it.cashRegisterId },
            title = { registerTitle(it, registers, texts) },
            chosen = state.register != null,
            onSelect = onPick
        )
    }
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

/** Как названа касса в отборе; `null` — все кассы. */
private fun registerTitle(id: String?, registers: List<ExchangeAddress>, texts: AnalyticsTexts): String =
    id?.let { chosen -> registers.firstOrNull { it.cashRegisterId == chosen }?.title }
        ?: texts.allRegisters
