package kz.mybrain.superkassa.presentation.users.signin

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.texport.superkassa.core.presentation.api.model.kkm.KkmResponse
import kz.mybrain.superkassa.domain.kkm.model.isAutonomous
import kz.mybrain.superkassa.domain.kkm.model.isBlocked
import kz.mybrain.superkassa.domain.kkm.model.orgAddress
import kz.mybrain.superkassa.domain.kkm.model.orgTitle
import kz.mybrain.superkassa.presentation.common.adaptive.WrapRow
import kz.mybrain.superkassa.presentation.common.list.RecordRow
import kz.mybrain.superkassa.presentation.common.list.ScrollableList
import kz.mybrain.superkassa.presentation.common.status.Chip
import kz.mybrain.superkassa.presentation.common.strings.LocalStrings
import kz.mybrain.superkassa.presentation.theme.StatusColors
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs
import kz.mybrain.superkassa.strings.api.common.LoginStrings

/**
 * Список касс на экране входа: поиск, строки и подробности кассы.
 *
 * Отделён от самого экрана входа: тот отвечает за вход — пин, кнопки
 * дверей, состояние узла, — а здесь живёт выбор кассы из списка.
 * В одном файле обе темы не помещались на экран разработчика.
 */

/**
 * Перечень касс.
 *
 * Одна карточка на весь список, а не карточка на строку: по Material 3
 * выбор из однородных значений — это список со строками, а рамка вокруг
 * каждой строки делает десяток касс похожим на десяток разных разделов.
 */
@Composable
internal fun KkmList(state: LoginUiState, modifier: Modifier = Modifier, onPick: (KkmResponse) -> Unit) {
    val kkms = state.shown
    val chosenId = state.chosen?.kkmId
    val list = rememberChosenInView(kkms, chosenId)

    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        ScrollableList(state = list, modifier = Modifier.fillMaxWidth().selectableGroup()) {
            items(kkms) { kkm ->
                KkmRow(
                    kkm = kkm,
                    name = state.nameOf(kkm),
                    selected = kkm.kkmId == chosenId,
                    remembered = kkm.kkmId == state.rememberedId,
                    onPick = { onPick(kkm) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

/**
 * Прокрутка списка к выбранной кассе — один раз, когда список пришёл.
 *
 * Касса прошлого раза часто стоит ниже видимой части, и без этого кассир
 * видел внизу номер своей кассы, а в списке ни одной отмеченной строки.
 * На выбор кассира прокрутка не отвечает: строка уезжала из-под пальца
 * в тот самый миг, когда по ней попали.
 */
@Composable
private fun rememberChosenInView(kkms: List<KkmResponse>, chosenId: String?): LazyListState {
    val list = rememberLazyListState()
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(kkms.isEmpty()) {
        if (kkms.isEmpty() || shown) return@LaunchedEffect
        shown = true
        val at = kkms.indexOfFirst { it.kkmId == chosenId }
        if (at >= 0) list.scrollToItem(at)
    }
    return list
}

@Composable
private fun KkmRow(
    kkm: KkmResponse,
    name: String,
    selected: Boolean,
    remembered: Boolean,
    onPick: () -> Unit
) {
    val texts = LocalStrings.current
    // Название — в две строки: у полусотни касс с общим началом названия
    // одна строка оставляла от каждой одинаковое «Касса торгового зала…».
    RecordRow(
        title = name,
        titleLines = NAME_LINES,
        subtitle = kkmDetail(kkm, texts.login),
        selected = selected,
        leading = { RadioButton(selected = selected, onClick = null) },
        modifier = Modifier.selectable(selected = selected, onClick = onPick),
        trailing = {
            WrapRow {
                if (remembered) Chip(texts.login.yourKkm, StatusColors.delivered)
                if (kkm.isBlocked) Chip(texts.shell.blocked, StatusColors.refused)
                if (kkm.isAutonomous) Chip(texts.shell.autonomous, StatusColors.pending)
            }
        }
    )
}

/**
 * Чем эта касса отличается от соседней в списке.
 *
 * Первой строкой идёт название кассы, подписью — её номер в КГД, владелец
 * и адрес установки. Номер подписан словом: без подписи он читался как
 * часть названия, а кассир не понимал, какая из строк — его касса.
 */
internal fun kkmDetail(kkm: KkmResponse, texts: LoginStrings): String = listOfNotNull(
    kkmNumber(kkm, texts),
    kkm.orgTitle,
    kkm.orgAddress,
    kkm.factoryNumber?.let { "${texts.factory} $it" }
).joinToString(Glyphs.SEPARATOR)

/**
 * Регистрационный номер КГД с подписью.
 *
 * Пусто, пока касса не поставлена на учёт: номера у неё ещё нет,
 * а подпись без числа обещает то, чего нет.
 */
internal fun kkmNumber(kkm: KkmResponse, texts: LoginStrings): String? =
    kkm.kkmKgdId?.takeIf { it.isNotBlank() }?.let { "${texts.registrationNumber} $it" }

/** Сколько строк отдаётся названию кассы в списке входа. */
private const val NAME_LINES = 2
