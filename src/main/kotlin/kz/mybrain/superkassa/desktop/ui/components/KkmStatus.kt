package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.server.Dictionary
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.strings.coreTexts
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kz.mybrain.superkassa.desktop.ui.theme.StatusColors

/**
 * Состояние кассы — одним набором плашек и в одном месте.
 *
 * Раньше состояние кассы стояло в настройках, режим программирования —
 * в диагностике, автономная работа и связь с узлом — в шапке: кассир
 * собирал картину по трём экранам, а одно и то же слово в двух местах
 * могло разойтись. Набор объявлен здесь один раз и показывается там,
 * где нужен, — в шапке окна.
 *
 * Порядок от тревожного к обычному: блокировку и автономную работу
 * кассир обязан увидеть первыми.
 */
@Composable
fun KkmStatusChips(session: Session) {
    val texts = LocalStrings.current
    val core = coreTexts(session.language)
    val kkm = session.selected
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.tight),
        verticalArrangement = Arrangement.spacedBy(Spacing.hairline),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        if (kkm?.isBlocked == true) Chip(texts.shell.blocked, StatusColors.refused)
        if (kkm?.isAutonomous == true) Chip(texts.shell.autonomous, StatusColors.pending)
        if (kkm != null) {
            Chip(
                text = session.titleOf(Dictionary.KkmStates, kkm.state),
                color = kkmStateColor(kkm.isBlocked, kkm.isProgramming)
            )
            Chip(
                text = if (session.shiftOpen) core.shiftOpenShort else core.shiftClosedShort,
                color = if (session.shiftOpen) StatusColors.delivered else StatusColors.pending
            )
        }
        Chip(
            text = if (session.nodeAvailable) texts.common.nodeOnline else texts.common.nodeOffline,
            color = if (session.nodeAvailable) StatusColors.delivered else StatusColors.refused
        )
    }
}

/** Цвет состояния кассы: блокировка — отказ, программирование — ожидание. */
@Composable
fun kkmStateColor(blocked: Boolean, programming: Boolean): Color = when {
    blocked -> StatusColors.refused
    programming -> StatusColors.pending
    else -> StatusColors.delivered
}
