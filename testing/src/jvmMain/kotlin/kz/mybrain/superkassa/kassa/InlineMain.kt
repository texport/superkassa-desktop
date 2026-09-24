package kz.mybrain.superkassa.kassa

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Выполняет [block] с главным потоком, который исполняет работу сразу.
 *
 * Модели экранов запускают работу в главном потоке окна; в проверке окна
 * нет, и без подмены ответ кассы доезжал бы до кадра когда придётся.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> inlineMain(block: () -> T): T {
    Dispatchers.setMain(Dispatchers.Unconfined)
    return try {
        block()
    } finally {
        Dispatchers.resetMain()
    }
}
