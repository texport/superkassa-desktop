package kz.mybrain.superkassa.integrations.maps.wire

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Пауза между обращениями к поиску.
 *
 * Поиск отдан сообществом, и его правила — не чаще раза в секунду. У сети
 * бывают сотни точек, и залпом их спрашивать нельзя: обращения встают
 * в очередь с паузой. Уже найденное в очередь не встаёт — его отдаёт память.
 */
internal class SearchPace(private val pause: () -> Duration, private val clock: TimeSource) {
    private val turn = Mutex()
    private var last: TimeMark? = null

    suspend fun <T> next(block: suspend () -> T): T = turn.withLock {
        val wait = last?.let { pause() - it.elapsedNow() } ?: Duration.ZERO
        if (wait.isPositive()) delay(wait)
        try {
            block()
        } finally {
            last = clock.markNow()
        }
    }
}
