package kz.mybrain.superkassa.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.ShiftState
import kz.mybrain.superkassa.desktop.ui.adaptive.MoneyText
import kz.mybrain.superkassa.desktop.ui.adaptive.TwoPane
import kz.mybrain.superkassa.desktop.ui.adaptive.WrapRow
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.moneyTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.KassaLayout
import kz.mybrain.superkassa.desktop.ui.theme.MoneyStyle
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Главный экран: состояние выбранной кассы и документы текущей смены.
 *
 * Показывается именно смена, а не весь журнал: кассиру в течение дня нужна
 * своя смена, а история — отдельный раздел.
 */
@Composable
fun DashboardScreen(session: Session) {
    val texts = LocalStrings.current
    val kkm = session.selected
    Column(
        modifier = Modifier.fillMaxSize().padding(Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(Spacing.normal)
    ) {
        if (kkm == null) {
            Text(texts.shell.noKkm, style = MaterialTheme.typography.titleMedium)
            Text(
                texts.login.pickHint,
                style = MaterialTheme.typography.bodyMedium
            )
            return@Column
        }
        StatTiles(session)

        AutonomousCard(session)

        ShiftActions(session)

        ShiftBody(session, Modifier.weight(1f))
    }
}

/**
 * Числа смены плитками.
 *
 * Плитки переносятся, а не сжимаются: остаток ящика в миллиарды в трети
 * узкого окна переносился посреди числа. Плитки одного ряда одной высоты,
 * хотя остаток набран крупнее соседей.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatTiles(session: Session) {
    val texts = LocalStrings.current
    WrapRow(modifier = Modifier.fillMaxWidth(), spacing = Spacing.normal) {
        val tile = Modifier.weight(1f).widthIn(min = KassaLayout.statTile).fillMaxRowHeight()
        val drawer = Modifier.weight(KassaLayout.DRAWER_TILE_WEIGHT).widthIn(min = KassaLayout.drawerTile)
        // Состояние кассы и смены стоит в шапке и повторено здесь не будет:
        // одно и то же слово в двух местах экрана расходится на первой же
        // правке. Плиткам остаются числа смены.
        // Номер смены берётся из ответа узла о смене, а не из записи
        // кассы: у кассы номер последней смены отстаёт, а плитка
        // называет ту смену, которую узел держит сейчас.
        StatCard(texts.dashboard.shift, tile) { StatText(shiftValue(session)) }
        // Плитка, карточка ящика и подтверждение Z-отчёта называют
        // остаток одним именем: три названия одного числа кассир читал
        // как три разных счётчика. Набран он тем же начертанием, что
        // в «Деньгах», — главным числом, одной строкой.
        StatCard(moneyTexts(session.language).drawer.inDrawer, drawer.fillMaxRowHeight()) {
            MoneyText(Money.formatTiyn(session.cashInDrawer), Modifier.fillMaxWidth(), MoneyStyle.hero)
        }
        // Число документов — только там, где узел их назвал. Непрочитанный
        // список показывался нулём, и «за смену не пробито ничего»
        // стояло над сменой, документы которой узел отдать отказался.
        StatCard(texts.dashboard.documentsInShift, tile) {
            StatText(if (session.documentsRead) session.documents.size.toString() else Glyphs.DASH)
        }
    }
}

/**
 * Отклонённые БФД и документы смены.
 *
 * Пока отказов нет, списку смены отдано всё место. Есть — они стоят
 * рядом со списком, а на тесном месте над ним, но список берёт свою долю
 * высоты первым: пачка отказов выдавливала документы смены за нижний
 * край окна.
 */
@Composable
private fun ShiftBody(session: Session, modifier: Modifier) {
    if (session.documents.all { it.printable }) {
        ShiftDocuments(session, modifier)
        return
    }
    TwoPane(
        split = KassaLayout.refusedAndDocuments,
        modifier = modifier.fillMaxWidth(),
        first = { RefusedDocuments(session) },
        second = { ShiftDocuments(session, Modifier.fillMaxSize()) }
    )
}

/** Состояние смены словами узла; неизвестное состояние так и называется. */
@Composable
private fun shiftValue(session: Session): String {
    val texts = LocalStrings.current
    return when (session.shiftState) {
        ShiftState.Open -> texts.dashboard.shiftOpenNo.format(session.shiftNumber ?: Glyphs.DASH)
        ShiftState.Closed -> texts.dashboard.shiftClosed
        ShiftState.Unknown -> texts.dashboard.shiftUnknown
    }
}

@Composable
private fun StatCard(caption: String, modifier: Modifier, value: @Composable () -> Unit) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(Spacing.normal), verticalArrangement = Arrangement.spacedBy(Spacing.hairline)) {
            Text(
                caption,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            value()
        }
    }
}

@Composable
private fun StatText(value: String) {
    Text(value, style = MaterialTheme.typography.headlineSmall)
}
