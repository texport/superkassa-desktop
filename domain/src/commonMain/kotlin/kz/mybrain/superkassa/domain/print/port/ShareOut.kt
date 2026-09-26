package kz.mybrain.superkassa.domain.print.port

import kz.mybrain.superkassa.domain.print.model.ShareWay
import kz.mybrain.superkassa.domain.print.model.SharedReceipt

/**
 * Чем эта машина делится чеком: окно «Поделиться» системы или ссылки
 * мессенджеров и почты.
 *
 * Отправляет не касса: она только отдаёт файл и слова программе, которой
 * покупателю пишут и так, — и кассир видит, кому и что уходит.
 */
interface ShareOut {

    /** Пути этой машины по порядку; пусто — делиться нечем. */
    val ways: List<ShareWay>

    /** Отдаёт чек пути [way]; `false` — система ничего не открыла. */
    suspend fun share(receipt: SharedReceipt, way: ShareWay): Boolean
}

/** Делиться нечем: сборка, где ни окна «Поделиться», ни ссылок нет. */
object NoShare : ShareOut {
    override val ways: List<ShareWay> = emptyList()

    override suspend fun share(receipt: SharedReceipt, way: ShareWay): Boolean = false
}
