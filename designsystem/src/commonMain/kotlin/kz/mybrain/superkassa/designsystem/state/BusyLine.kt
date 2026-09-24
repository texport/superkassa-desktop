package kz.mybrain.superkassa.designsystem.state

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.designsystem.theme.size.Sizes

/**
 * Полоска ожидания под шапкой.
 *
 * Один индикатор на всё окно, а не свой у каждой кнопки: обращение к кассе
 * идёт из любого раздела, и кассир должен видеть, что касса занята,
 * не гадая, какая кнопка сейчас работает. Место постоянное — полоска
 * не сдвигает содержимое, когда появляется.
 */
@Composable
fun BusyLine(busy: Boolean) {
    Box(modifier = Modifier.fillMaxWidth().height(Sizes.busyLine)) {
        // Пауза перед показом — та же, что у ожидания на месте содержимого:
        // касса в процессе отвечает за десятки миллисекунд, и полоска
        // мигала бы на каждом нажатии.
        if (waitedLongEnough(busy)) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}
