package kz.mybrain.superkassa.desktop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kz.mybrain.superkassa.desktop.ui.strings.LocalStrings
import kz.mybrain.superkassa.desktop.ui.theme.Durations
import kz.mybrain.superkassa.desktop.ui.theme.Sizes
import kz.mybrain.superkassa.desktop.ui.theme.Spacing

/**
 * Ожидание ответа — одно на всё приложение.
 *
 * Стоит на месте содержимого, а не поверх него: пока ответа нет,
 * показывать всё равно нечего, а кружок поверх пустоты закрывает собой
 * то самое место, куда человек смотрит. Полоска под шапкой отвечает
 * за другое — за то, что касса занята, пока работают в другом разделе.
 *
 * Своих было два — у журнала и у аналитики, — и они разошлись размером
 * значка, шкалой подписи и отступами. Теперь их нет: экран объявляет
 * состояние и отдаёт его [ScreenSlot], а рисуется оно здесь.
 *
 * Кружок появляется не сразу: ответ с той же машины приходит за десятки
 * миллисекунд, и мелькание на каждом нажатии раздражает сильнее пустоты.
 * Пауза берётся из [Durations] и одна на все экраны. Уходит ожидание
 * вместе с самим элементом: сняли с экрана — и отсчёт отменился, вечно
 * крутиться нечему.
 *
 * @param dense плотный вид для списка внутри карточки.
 */
@Composable
fun LoadingState(modifier: Modifier = Modifier, dense: Boolean = false) {
    if (!waitedLongEnough(true)) return
    val texts = LocalStrings.current
    Column(
        modifier = modifier.fillMaxWidth().padding(Spacing.roomy),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(
            if (dense) Spacing.tight else Spacing.snug,
            Alignment.CenterVertically
        )
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(if (dense) Sizes.waitCircleDense else Sizes.waitCircle)
        )
        Text(
            text = texts.common.loading,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Та же пауза для тех мест, где ожидание рисуют не кружком.
 *
 * Полоска под шапкой живёт всё время, пока открыто окно, и своим
 * появлением не управляет — поэтому отсчёт ей нужен отдельной строкой,
 * а не уходом элемента с экрана.
 */
@Composable
fun waitedLongEnough(working: Boolean): Boolean {
    val shown by produceState(initialValue = false, working) {
        value = false
        if (!working) return@produceState
        delay(Durations.beforeWaiting)
        value = true
    }
    return shown
}
