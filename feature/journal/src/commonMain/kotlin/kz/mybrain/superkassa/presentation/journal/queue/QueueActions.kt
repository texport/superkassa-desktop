package kz.mybrain.superkassa.presentation.journal.queue

/**
 * Что кассир может сделать с очередью.
 *
 * Действия по умолчанию пустые — для снимков вида, где нажимать некому.
 */
interface QueueActions {
    fun refresh() = Unit

    fun retryFailed() = Unit
}
