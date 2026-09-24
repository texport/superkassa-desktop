package kz.mybrain.superkassa.presentation.common.image

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap

/**
 * Картинка из её байтов: плитка карты, печатная форма.
 *
 * Разбирает картинку платформа: у настольных систем это Skia, у Android —
 * свой разборщик. Неразборчивые байты — `null`: экран говорит, что
 * картинки нет, а не падает.
 */
expect fun encodedImage(bytes: ByteArray): ImageBitmap?

/**
 * Масштаб колесом мыши с Ctrl — как в любом просмотрщике.
 *
 * Колесо без Ctrl прокручивает: длинный Z-отчёт листают чаще, чем меняют
 * масштаб. Ctrl-щелчок разбирается раньше прокрутки и поглощается: без
 * этого одно движение колеса и меняло масштаб, и уезжало по ленте.
 * На телефоне колеса нет, и модификатор ничего не добавляет.
 *
 * @param onZoom на сколько щелчков колеса увеличить: вверх — больше нуля.
 */
expect fun Modifier.zoomByWheel(onZoom: (Float) -> Unit): Modifier
