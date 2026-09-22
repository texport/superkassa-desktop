package kz.mybrain.superkassa.desktop.ui.cabinet

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kz.mybrain.superkassa.desktop.eds.NcaLayer
import kz.mybrain.superkassa.desktop.ui.strings.CabinetTexts
import kz.mybrain.superkassa.desktop.ui.strings.Language
import kz.mybrain.superkassa.desktop.ui.strings.edsTexts
import kz.mybrain.superkassa.desktop.ui.theme.Durations
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Ожидание подписи заявления — то же, что на двери входа.
 *
 * Подпись приложение просит в трёх местах: на входе по ЭЦП, при подаче
 * заявления из кабинета и при постановке на учёт в мастере подключения.
 * На двери давно стоит [SignWait] с видимым сроком и отменой, а в двух
 * остальных местах была занятая кнопка: владелец так же смотрел
 * в неподвижный экран до трёх минут, не зная, сколько осталось,
 * и не мог прервать ожидание.
 *
 * Отсчёт начинается с появления на экране: ожидание и показывается ровно
 * тогда, когда подпись уже запрошена.
 *
 * @param onCancel прерывает саму подачу. Отменённая подача в кабинет
 *   ничего не отправляет: подпись стоит перед отправкой, и отмена
 *   до неё не доходит.
 */
@Composable
fun ApplicationSignWait(language: Language, texts: CabinetTexts, onCancel: () -> Unit) {
    SignWait(
        left = signWaitLeft(LocalSignTick.current),
        window = NcaLayer.SIGN_WINDOW,
        texts = texts,
        eds = edsTexts(language),
        onCancel = onCancel
    )
}

/**
 * Как часто пересчитывается остаток.
 *
 * Владельцу хватает раза в секунду. Проверке нужен шаг мельче: движение
 * отсчёта она ловит ожиданием по стенным часам, и под нагрузкой соседних
 * прогонов секунда не укладывалась в отведённое ей время — проверка падала
 * на исправном коде.
 */
val LocalSignTick: ProvidableCompositionLocal<Duration> =
    staticCompositionLocalOf { Durations.everySecond }

/**
 * Сколько ожидания осталось.
 *
 * Срок берётся у самого [NcaLayer]: считать его здесь своим значением
 * значит однажды показать владельцу время, которого у него нет.
 */
@Composable
private fun signWaitLeft(tick: Duration): Duration {
    var left by remember { mutableStateOf(NcaLayer.SIGN_WINDOW) }
    LaunchedEffect(tick) {
        val since = TimeSource.Monotonic.markNow()
        while (isActive) {
            left = (NcaLayer.SIGN_WINDOW - since.elapsedNow()).coerceAtLeast(Duration.ZERO)
            delay(tick)
        }
    }
    return left
}
