package kz.mybrain.superkassa.presentation.analytics.kkm

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kz.mybrain.superkassa.domain.analytics.model.AnalyticsKkm
import kz.mybrain.superkassa.presentation.analytics.common.shiftPlate
import kz.mybrain.superkassa.presentation.analytics.map.MapWords
import kz.mybrain.superkassa.presentation.analytics.sales.AnalyticsSales
import kz.mybrain.superkassa.presentation.analytics.sales.kkmSalesViewModel
import kz.mybrain.superkassa.presentation.common.keyboard.onEscape
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.theme.icon.Glyphs
import kz.mybrain.superkassa.presentation.theme.motion.Durations
import kz.mybrain.superkassa.presentation.theme.size.Sizes
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import kz.mybrain.superkassa.strings.api.analytics.AnalyticsTexts
import kz.mybrain.superkassa.strings.api.cabinet.CabinetTexts

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
    app: AppContainer,
    kkm: AnalyticsKkm,
    access: String?,
    words: MapWords,
    onClose: () -> Unit
) {
    val texts = words.texts
    val cabinetTexts = words.cabinetTexts
    val model = kkmSalesViewModel(app, kkm.cashRegisterId)
    LaunchedEffect(model, access) {
        model.follow(access)
        model.watch(Durations.whileWatching)
    }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        KkmSalesSurface(onClose) {
            DialogHead(kkm, texts, cabinetTexts, onClose)
            AnalyticsSales(model, texts, cabinetTexts, Modifier.weight(1f))
        }
    }
}

/**
 * Поверхность окна.
 *
 * Своего размера окно не больше окна кассы: в наименьшем окне 960×640
 * окно высотой 680 уходило низом за край.
 */
@Composable
private fun KkmSalesSurface(onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .padding(Spacing.blockPadding)
            .sizeIn(maxWidth = Sizes.kkmSalesWidth, maxHeight = Sizes.kkmSalesHeight)
            .fillMaxSize()
            .onEscape {
                onClose()
                true
            },
        shape = RoundedCornerShape(Sizes.corner),
        tonalElevation = Sizes.dialogElevation
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(Spacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap),
            content = content
        )
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
        horizontalArrangement = Arrangement.spacedBy(Spacing.buttonGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${texts.kkmSalesTitle}${Glyphs.SEPARATOR}${kkm.title}",
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val about = kkmAbout(kkm, cabinet)
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

/**
 * Строка под названием кассы: смена, торговая точка и адрес.
 *
 * Смена — свойство самой кассы: та же строка, что и в учёте, а не число
 * сводки кабинета, которое у кассы с открытой сменой говорило «0». Стоит
 * она первой: строка обрывается многоточием, и в окне 960×640 длинный
 * адрес точки уносил смену за край — открытая смена в окне кассы
 * не была видна вовсе.
 */
internal fun kkmAbout(kkm: AnalyticsKkm, cabinet: CabinetTexts): String =
    listOfNotNull(shiftPlate(kkm.shiftStatus, kkm.shiftNumber, cabinet), kkm.retailPlaceName, kkm.address)
        .joinToString(Glyphs.SEPARATOR)
