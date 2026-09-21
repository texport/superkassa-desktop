package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.server.cabinet.ExchangeAddress
import kz.mybrain.superkassa.desktop.ui.components.CounterTile
import kz.mybrain.superkassa.desktop.ui.components.InfoTip
import kz.mybrain.superkassa.desktop.ui.components.MenuChip
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.components.ScreenSlot
import kz.mybrain.superkassa.desktop.ui.components.ScreenState
import kz.mybrain.superkassa.desktop.ui.components.SectionTitle
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

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
fun AnalyticsExchangePane(cabinet: CabinetSession, texts: AnalyticsTexts) {
    val model = remember(cabinet) { AnalyticsExchangeModel(cabinet) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(cabinet.token) { model.load() }
    val all = model.view?.addresses.orEmpty()
    val rows = exchangeRows(all, model.query, model.register)

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.snug)
    ) {
        ExchangeHead(model, texts) { scope.launch { model.load() } }
        ExchangeFilters(model, all, texts)
        ExchangeBody(model, rows, all.isEmpty(), texts, Modifier.weight(1f)) { scope.launch { model.load() } }
    }
}

/** Заголовок раздела, счётчики кабинета и обновление. */
@Composable
private fun ExchangeHead(model: AnalyticsExchangeModel, texts: AnalyticsTexts, onRefresh: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.normal),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle(texts.exchangeTitle)
        InfoTip(texts.exchangeHint)
        Spacer(Modifier.weight(1f))
        CounterTile(Money.count((model.view?.cashRegisterCount ?: 0)), texts.kkmCount)
        CounterTile(Money.count((model.view?.addressCount ?: 0)), texts.addressCount)
        IconButton(onClick = onRefresh, enabled = !model.loading) {
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
 */
@Composable
private fun ExchangeFilters(model: AnalyticsExchangeModel, all: List<ExchangeAddress>, texts: AnalyticsTexts) {
    val registers = exchangeRegisters(all)
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.snug),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = model.query,
            onValueChange = { model.query = it },
            label = { Text(texts.search) },
            singleLine = true,
            modifier = Modifier.width(Sizes.fieldSearch)
        )
        MenuChip(
            value = registerTitle(model.register, registers, texts),
            options = listOf<String?>(null) + registers.map { it.cashRegisterId },
            title = { registerTitle(it, registers, texts) },
            chosen = model.register != null,
            onSelect = { model.register = it }
        )
    }
}

/** Помеха, ожидание, пустота или сам список. */
@Composable
private fun ExchangeBody(
    model: AnalyticsExchangeModel,
    rows: List<ExchangeAddress>,
    nothingAtAll: Boolean,
    texts: AnalyticsTexts,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit
) {
    val trouble = model.trouble
    val state = when {
        trouble != null -> analyticsTroubleState(trouble, texts, onRetry)
        model.view == null -> ScreenState.Working
        nothingAtAll -> ScreenState.Empty(AppIcons.noDocuments, texts.exchangeEmpty, texts.exchangeEmptyHint)
        rows.isEmpty() -> ScreenState.Empty(AppIcons.find, texts.exchangeNotFound, texts.exchangeNotFoundHint)
        else -> ScreenState.Ready
    }
    ScreenSlot(state, modifier) { AnalyticsExchangeList(rows, texts, modifier) }
}

/** Как названа касса в отборе; `null` — все кассы. */
private fun registerTitle(id: String?, registers: List<ExchangeAddress>, texts: AnalyticsTexts): String =
    id?.let { chosen -> registers.firstOrNull { it.cashRegisterId == chosen }?.let(::kkmTitle) }
        ?: texts.allRegisters
