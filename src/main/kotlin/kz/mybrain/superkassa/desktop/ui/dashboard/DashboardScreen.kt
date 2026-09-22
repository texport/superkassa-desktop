package kz.mybrain.superkassa.desktop.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.app.ShiftState
import kz.mybrain.superkassa.desktop.ui.components.Money
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.moneyTexts
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
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
        modifier = Modifier.fillMaxWidth().padding(Spacing.screen),
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
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.normal), modifier = Modifier.fillMaxWidth()) {
            // Состояние кассы и смены стоит в шапке и повторено здесь не будет:
            // одно и то же слово в двух местах экрана расходится на первой же
            // правке. Плиткам остаются числа смены.
            // Номер смены берётся из ответа узла о смене, а не из записи
            // кассы: у кассы номер последней смены отстаёт, а плитка
            // называет ту смену, которую узел держит сейчас.
            StatCard(
                caption = texts.dashboard.shift,
                value = shiftValue(session),
                modifier = Modifier.weight(1f)
            )
            // Плитка, карточка ящика и подтверждение Z-отчёта называют
            // остаток одним именем: три названия одного числа кассир читал
            // как три разных счётчика.
            StatCard(
                caption = moneyTexts(session.language).drawer.inDrawer,
                value = Money.formatTiyn(session.cashInDrawer),
                modifier = Modifier.weight(1f)
            )
            // Число документов — только там, где узел их назвал. Непрочитанный
            // список показывался нулём, и «за смену не пробито ничего»
            // стояло над сменой, документы которой узел отдать отказался.
            StatCard(
                caption = texts.dashboard.documentsInShift,
                value = if (session.documentsRead) session.documents.size.toString() else Glyphs.DASH,
                modifier = Modifier.weight(1f)
            )
        }

        AutonomousCard(session)

        ShiftActions(session)

        RefusedDocuments(session)

        ShiftDocuments(session)
    }
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
private fun StatCard(caption: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(Spacing.normal), verticalArrangement = Arrangement.spacedBy(Spacing.hairline)) {
            Text(
                caption,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(value, style = MaterialTheme.typography.headlineSmall)
        }
    }
}
