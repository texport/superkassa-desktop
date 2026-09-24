package kz.mybrain.superkassa.presentation.print.preview

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.domain.print.port.PrintPorts
import kz.mybrain.superkassa.presentation.common.model.WindowServices
import kz.mybrain.superkassa.presentation.common.model.collectAsScreenState
import kz.mybrain.superkassa.presentation.common.print.LocalPrint
import kz.mybrain.superkassa.presentation.print.preview.component.PrintOverlay

/**
 * Стол печати: содержимое окна и печатная форма поверх него.
 *
 * Форма живёт над всеми разделами: кассир открывает её из журнала и вправе
 * уйти в продажу, не теряя окна. То же окно стоит и над дверью в кабинет —
 * оно одно на оба входа. Разделы зовут печать через [LocalPrint]
 * и о модели печати не знают.
 */
@Composable
fun PrintDesk(services: WindowServices, ports: PrintPorts, content: @Composable () -> Unit) {
    val model = printViewModel(services, ports)
    val paper by model.state.collectAsScreenState()
    val print = remember(model) { model.actions() }
    val window = remember(model) { model.paperActions() }
    CompositionLocalProvider(LocalPrint provides print) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            PrintOverlay(paper, window)
        }
    }
}
