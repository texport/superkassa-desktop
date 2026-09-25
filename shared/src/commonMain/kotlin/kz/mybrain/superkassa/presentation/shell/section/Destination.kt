package kz.mybrain.superkassa.presentation.shell.section

import androidx.compose.ui.graphics.vector.ImageVector
import kz.mybrain.superkassa.strings.api.common.SectionTexts

/**
 * Пункт навигации окна: значок и название.
 *
 * Окно одно, а наборов разделов два: рабочий ([Section]) — после входа,
 * и окно до входа ([DoorSection]). Рамка окна рисует любой из них одной
 * навигацией Material 3.
 */
internal interface Destination {
    val icon: ImageVector
    val title: (SectionTexts) -> String
}
