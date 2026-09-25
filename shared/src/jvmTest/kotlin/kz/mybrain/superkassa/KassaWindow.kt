package kz.mybrain.superkassa

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.presentation.common.message.MessageHost
import kz.mybrain.superkassa.presentation.shell.bar.ShellBar
import kz.mybrain.superkassa.presentation.shell.frame.ShellFrame
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
    ShellFrame(
        sections = Section.entries,
        current = section,
        onPick = {},
        topBar = { onMenu -> ShellBar(desk.parts, shell, section, onSignOut = {}, onMenu = onMenu) },
        snackbarHost = { messages?.let { MessageHost(it) } }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.sectionFrame().fillMaxHeight()) { content() }
        }
    }
}
