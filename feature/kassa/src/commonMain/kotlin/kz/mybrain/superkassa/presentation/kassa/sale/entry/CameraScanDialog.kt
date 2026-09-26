package kz.mybrain.superkassa.presentation.kassa.sale.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kz.mybrain.superkassa.designsystem.keyboard.CloseOnEscape
import kz.mybrain.superkassa.designsystem.state.EmptyState
import kz.mybrain.superkassa.designsystem.strings.LocalLanguage
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.presentation.common.scan.CameraAccess
import kz.mybrain.superkassa.presentation.common.scan.CodeCamera
import kz.mybrain.superkassa.strings.api.kassa.scan.CameraScanTexts
import kz.mybrain.superkassa.strings.api.textsOf

/**
 * Сканер штрихкода камерой — полноэкранный диалог Material 3.
 *
 * Третий способ набрать код, рядом с цифрами и буквами: у кассы на
 * телефоне и планшете сканера бывает нет, а камера есть всегда. Найденный
 * код ложится в поле и ищется, как после сканера с Enter; окно закрывается
 * само. Разрешение на камеру объясняется здесь же, до системного вопроса,
 * а отказанное насовсем — ведёт в настройки приложения.
 *
 * @param onCode код прочитан: поле его примет и начнёт поиск.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CameraScanDialog(camera: CodeCamera, onCode: (String) -> Unit, onDismiss: () -> Unit) {
    val texts = textsOf(LocalLanguage.current).kassa.scan
    CloseOnEscape(onDismiss)
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(modifier = Modifier.fillMaxSize(), shape = RectangleShape) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = onDismiss) { Icon(AppIcons.close, contentDescription = texts.close) }
                    },
                    title = { Text(texts.title) }
                )
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    ScanBody(camera, camera.access(), texts, onCode)
                }
            }
        }
    }
}

/** Видоискатель с подсказкой под ним — или просьба о разрешении. */
@Composable
private fun ScanBody(camera: CodeCamera, access: CameraAccess, texts: CameraScanTexts, onCode: (String) -> Unit) {
    when (access) {
        CameraAccess.Granted -> Column(modifier = Modifier.fillMaxSize()) {
            camera.Viewfinder(onCode, Modifier.weight(1f).fillMaxWidth())
            Text(
                text = texts.hint,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth().padding(Spacing.blockPadding)
            )
        }
        is CameraAccess.Missing -> AccessAsk(texts.why, texts.allow, access.ask)
        is CameraAccess.Denied -> AccessAsk(texts.denied, texts.settings, access.settings)
    }
}

/** Зачем кассе камера и одно действие — разрешить или открыть настройки. */
@Composable
private fun AccessAsk(why: String, action: String, onAction: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.fieldGap)
    ) {
        EmptyState(icon = AppIcons.camera, title = why)
        Button(onClick = onAction) { Text(action) }
    }
}
