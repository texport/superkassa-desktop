package kz.mybrain.superkassa.presentation.setup

import kotlinx.coroutines.CancellationException
import kz.mybrain.superkassa.presentation.common.cabinet.CabinetCalls

/**
 * Обращения к кабинету без кабинета окна: помеха не показывается, а записывается.
 *
 * Проверкам модели мастера окно кабинета не нужно: им важно, что помеха
 * обращения не прошла дальше и чем обращение кончилось.
 */
internal class DirectCalls : CabinetCalls {
    /** Какие обращения не удались, по порядку. */
    val failed = mutableListOf<String>()

    override suspend fun <T> run(action: String, block: suspend () -> T): T? = try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: IllegalStateException) {
        failed += "$action: ${failure.message}"
        null
    }
}
