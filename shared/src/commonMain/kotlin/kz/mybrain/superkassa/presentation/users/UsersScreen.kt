package kz.mybrain.superkassa.presentation.users

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.common.adaptive.CardColumns
import kz.mybrain.superkassa.presentation.common.list.ScrollableColumn
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.section.ScreenTitle
import kz.mybrain.superkassa.presentation.common.section.SectionCard
import kz.mybrain.superkassa.presentation.common.state.ScreenSlot
import kz.mybrain.superkassa.presentation.common.state.ScreenState
import kz.mybrain.superkassa.presentation.strings.common.LocalLanguage
import kz.mybrain.superkassa.presentation.strings.common.LocalStrings
import kz.mybrain.superkassa.presentation.strings.kassa.MoneyTexts
import kz.mybrain.superkassa.presentation.strings.kassa.moneyTexts
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.size.Spacing

/** Кассиры кассы: состояние — из модели, действия — ей же. */
@Composable
fun UsersScreen(model: UsersViewModel) {
    val state by model.state.collectAsScreenState()
    // Список перечитывается каждый раз, как раздел открыт: кассира могли
    // завести с другого рабочего места, пока администратор был в журнале.
    LaunchedEffect(Unit) { model.reload() }
    UsersContent(state, model)
}

/**
 * Кассиры кассы: список, заведение, смена пина, удаление.
 *
 * Правила пинов не собраны в отдельную памятку, а стоят там, где кассир
 * с ними сталкивается: длина — подсказкой под полем пина, роли — под
 * выбором роли, запрет на удаление последнего — в строке того кассира,
 * которого не дают удалить.
 *
 * Раздел во всю ширину: на широком окне заведение и список стоят рядом,
 * и кнопки строки не уезжают от имени кассира на полторы тысячи точек.
 */
@Composable
fun UsersContent(state: UsersUiState, actions: UsersActions) {
    val texts = LocalStrings.current
    val language = LocalLanguage.current
    val money = moneyTexts(language)
    ScrollableColumn(
        modifier = Modifier.fillMaxSize().padding(Spacing.screen),
        spacing = Spacing.normal
    ) {
        ScreenTitle(texts.users.title)
        CardColumns(Modifier.fillMaxWidth()) {
            AddCashier(state, actions, money)
            SectionCard(title = money.cashiers.listTitle, info = money.cashiers.listHint) {
                ScreenSlot(listState(state, actions, money), dense = true) { UserRows(state, actions, money) }
            }
        }
    }
    state.pin?.let { change ->
        ChangePinDialog(money, change, roleWord(change.user.role, texts.users, state.roleNames, language), actions)
    }
    state.removing?.let { RemoveDialog(money, it, actions) }
}

@Composable
private fun UserRows(state: UsersUiState, actions: UsersActions, money: MoneyTexts) {
    val texts = LocalStrings.current
    val language = LocalLanguage.current
    state.users.forEachIndexed { index, user ->
        if (index > 0) HorizontalDivider()
        UserRow(
            money = money,
            roleTitle = roleWord(user.role, texts.users, state.roleNames, language),
            user = user,
            deletable = state.deletable(user),
            actions = actions
        )
    }
}

/**
 * Что стоит на месте списка.
 *
 * Отказ кассы назван словами и с повтором: пустая касса и касса, о кассирах
 * которой не спросить, — разные беды, и вторая выглядела пустой рамкой.
 */
private fun listState(state: UsersUiState, actions: UsersActions, money: MoneyTexts): ScreenState = when {
    state.users.isNotEmpty() -> ScreenState.Ready
    state.unreadable -> ScreenState.Trouble(
        title = money.cashiers.unreadable,
        hint = money.cashiers.unreadableHint,
        onRetry = actions::reload
    )

    !state.answered -> ScreenState.Working
    else -> ScreenState.Empty(AppIcons.cashiers, money.cashiers.empty, money.cashiers.emptyHint)
}
