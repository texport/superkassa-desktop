package kz.mybrain.superkassa.desktop.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kz.mybrain.superkassa.desktop.app.CabinetSession
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.cabinet.AnalyticsKkm
import kz.mybrain.superkassa.desktop.ui.components.onEscape
import kz.mybrain.superkassa.desktop.ui.strings.AnalyticsTexts
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons
import kz.mybrain.superkassa.desktop.ui.theme.Durations
import kz.mybrain.superkassa.desktop.ui.theme.Glyphs
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Вся аналитика одной кассы.
 *
 * Карта отвечала на вопрос «где стоят мои кассы» и на нём кончалась:
 * дойдя до нужной, владелец видел её реквизиты и шёл искать выручку
 * в сводке по всей сети, отбирая кассу там заново. Здесь она открывается
 * сразу: те же плитки, графики и таблицы, что и у сети, но посчитанные
 * по одной кассе.
 *
 * Числа обновляются сами, пока окно открыто: касса торгует сейчас,
 * и сводка, снятая при открытии, к концу разговора о ней уже неверна.
 * Обновление останавливается вместе с окном — своего потока оно
 * не держит.
 */
@Composable
fun AnalyticsKkmDialog(
    session: Session,
    cabinet: CabinetSession,
    kkm: AnalyticsKkm,
    texts: AnalyticsTexts,
    cabinetTexts: CabinetTexts,
    onClose: () -> Unit
) {
    val model = remember(kkm.cashRegisterId) { AnalyticsSalesModel(cabinet, kkm.cashRegisterId) }
    LaunchedEffect(model) {
        while (true) {
            delay(Durations.whileWatching)
            model.load()
        }
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier
                .width(Sizes.kkmSalesWidth)
                .height(Sizes.kkmSalesHeight)
                .onEscape {
                    onClose()
                    true
                },
            shape = RoundedCornerShape(Sizes.corner),
            tonalElevation = Sizes.dialogElevation
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(Spacing.normal),
                verticalArrangement = Arrangement.spacedBy(Spacing.snug)
            ) {
                DialogHead(kkm, texts, cabinetTexts, onClose)
                AnalyticsSales(session, model, texts, cabinetTexts, Modifier.weight(1f))
            }
        }
    }
}

/** Шапка окна: чья это касса и где она стоит. */
@Composable
private fun DialogHead(
    kkm: AnalyticsKkm,
    texts: AnalyticsTexts,
    cabinet: CabinetTexts,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${texts.kkmSalesTitle}${Glyphs.SEPARATOR}${kkmTitle(kkm)}",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val about = listOfNotNull(kkm.retailPlaceName, kkm.address).joinToString(" · ")
            if (about.isNotBlank()) {
                Text(
                    text = about,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = onClose) {
            Icon(AppIcons.close, contentDescription = cabinet.close)
        }
    }
}
