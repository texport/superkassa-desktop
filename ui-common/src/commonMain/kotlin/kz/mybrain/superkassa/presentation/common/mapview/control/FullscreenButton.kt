package kz.mybrain.superkassa.presentation.common.mapview.control

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.strings.api.map.MapTexts

/**
 * Кнопка «на весь экран» среди кнопок карты — одна на все карты.
 *
 * Карта в окне делит место с адресом, шагами регистра и подвалом, и в
 * небольшом окне на ней не различить соседние дома; раскрытая, она берёт
 * всё окно приложения. Та же кнопка возвращает её обратно.
 *
 * @param fullscreen раскрыта ли карта сейчас: от этого значок и подпись.
 */
@Composable
fun FullscreenButton(fullscreen: Boolean, texts: MapTexts, onToggle: () -> Unit) {
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (fullscreen) AppIcons.fullscreenExit else AppIcons.fullscreen,
            contentDescription = if (fullscreen) texts.fullscreenExit else texts.fullscreen
        )
    }
}
