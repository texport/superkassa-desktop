package kz.mybrain.superkassa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.common.message.MessageHost
import kz.mybrain.superkassa.presentation.shell.bar.ShellBar
import kz.mybrain.superkassa.presentation.shell.rail.SectionRail
import kz.mybrain.superkassa.presentation.shell.section.Section
import kz.mybrain.superkassa.presentation.shell.section.sectionFrame

/**
 * Рабочее окно: шапка, рельс и раздел в пределе рабочего экрана — как
 * в `WorkShell`, но раздел подаётся готовым, с набранным чеком.
 */
@Composable
internal fun KassaWindow(
    desk: KassaDesk,
    section: Section,
    messages: SnackbarHostState? = null,
    content: @Composable () -> Unit
) {
    val shell by desk.parts.shell.state.collectAsState()
    val look by desk.look.state.collectAsState()
    Scaffold(
        topBar = { ShellBar(desk.parts, shell, section, onSignOut = {}) },
        snackbarHost = { messages?.let { MessageHost(it) } }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            SectionRail(Section.entries, section, look.railCollapsed, {}, { Text("1.0.6") }) {}
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.sectionFrame().fillMaxHeight()) { content() }
            }
        }
    }
}
