package kz.mybrain.superkassa

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import io.ktor.http.HttpStatusCode
import kz.mybrain.superkassa.presentation.cabinet.applications.LocalSignTick
import kz.mybrain.superkassa.presentation.cabinet.steps
import kz.mybrain.superkassa.presentation.common.cabinet.CabinetSteps
import kotlin.time.Duration

/**
 * Шаги кабинета для мастера подключения — от настоящего окна кабинета,
 * как их отдаёт мастеру каркас окна.
 *
 * Мастер знает кабинет только через контракт шагов; проверкам мастера
 * окно кабинета нужно целиком, но видеть его модуль им незачем.
 */
object CabinetStepsRig {

    /** Шаги вошедшего кабинета, отвечающего [body] на любой запрос. */
    fun signedIn(body: String, status: HttpStatusCode = HttpStatusCode.OK): CabinetSteps =
        mockCabinet(body, status).steps()

    /** Шаги кабинета, в который никто не входил. */
    fun idle(): CabinetSteps = idleCabinet().steps()

    /** Содержимое, в котором ожидание подписи отсчитывает шагом [tick]. */
    @Composable
    fun Ticking(tick: Duration, content: @Composable () -> Unit) =
        CompositionLocalProvider(LocalSignTick provides tick, content = content)
}
