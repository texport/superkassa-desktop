package kz.mybrain.superkassa.desktop.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import kz.mybrain.superkassa.desktop.server.Kkm
import kz.mybrain.superkassa.desktop.ui.components.Chip
import kz.mybrain.superkassa.desktop.ui.components.RecordRow
import kz.mybrain.superkassa.desktop.ui.components.ScrollableList
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.LoginStrings
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

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
internal fun KkmList(
    kkms: List<Kkm>,
    nameOf: (Kkm) -> String,
    chosenId: String?,
    rememberedId: String?,
    modifier: Modifier = Modifier,
    onPick: (Kkm) -> Unit
) {
    val state = rememberLazyListState()
    var shown by remember { mutableStateOf(false) }

    // Список прокручивается к запомненной кассе один раз — когда пришёл
    // с узла. Она часто стоит ниже видимой части, и без этого кассир
    // видел внизу регистрационный номер своей кассы, а в списке ни одной
    // отмеченной строки. На выбор кассира прокрутка не отвечает: строка
    // уезжала из-под пальца в тот самый миг, когда по ней попали.
    LaunchedEffect(kkms.isEmpty()) {
        if (kkms.isEmpty() || shown) return@LaunchedEffect
        shown = true
        val at = kkms.indexOfFirst { it.kkmId == chosenId }
        if (at >= 0) state.scrollToItem(at)
    }

    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        ScrollableList(state = state, modifier = Modifier.fillMaxWidth().selectableGroup()) {
            items(kkms) { kkm ->
                KkmRow(
                    kkm = kkm,
                    name = nameOf(kkm),
                    selected = kkm.kkmId == chosenId,
                    remembered = kkm.kkmId == rememberedId,
                    onPick = { onPick(kkm) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun KkmRow(
    kkm: Kkm,
    name: String,
    selected: Boolean,
    remembered: Boolean,
    onPick: () -> Unit
) {
    val texts = LocalStrings.current
    RecordRow(
        title = name,
        subtitle = kkmDetail(kkm, texts.login),
        selected = selected,
        leading = { RadioButton(selected = selected, onClick = null) },
        modifier = Modifier.selectable(selected = selected, onClick = onPick),
        trailing = {
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.tight)) {
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
internal fun kkmDetail(kkm: Kkm, texts: LoginStrings): String = listOfNotNull(
    kkmNumber(kkm, texts),
    kkm.orgTitle.takeIf { it.isNotBlank() },
    kkm.orgAddress.takeIf { it.isNotBlank() },
    kkm.factoryNumber?.let { "${texts.factory} $it" }
).joinToString(Glyphs.SEPARATOR)

/**
 * Регистрационный номер КГД с подписью.
 *
 * Пусто, пока касса не поставлена на учёт: номера у неё ещё нет,
 * а подпись без числа обещает то, чего нет.
 */
internal fun kkmNumber(kkm: Kkm, texts: LoginStrings): String? =
    kkm.kkmKgdId?.takeIf { it.isNotBlank() }?.let { "${texts.registrationNumber} $it" }
