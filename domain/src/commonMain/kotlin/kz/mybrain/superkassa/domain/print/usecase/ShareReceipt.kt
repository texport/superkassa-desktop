package kz.mybrain.superkassa.domain.print.usecase

import kz.mybrain.superkassa.domain.print.model.ShareWay
import kz.mybrain.superkassa.domain.print.model.Shared
import kz.mybrain.superkassa.domain.print.model.SharedReceipt
import kz.mybrain.superkassa.domain.print.port.ShareOut

/**
 * Поделиться чеком: отдать файл и ссылку программе, которой пишут покупателю.
 *
 * Путь, по которому уходит только ссылка, без ссылки не открывается:
 * пустое сообщение в мессенджере покупателю ничего не даст, а кассир
 * решил бы, что чек отправлен.
 */
class ShareReceipt(private val share: ShareOut) {

    /** Пути этой машины по порядку; пусто — делиться нечем. */
    val ways: List<ShareWay> get() = share.ways

    suspend operator fun invoke(receipt: SharedReceipt, way: ShareWay): Shared = when {
        !way.carriesFile && receipt.link == null -> Shared.NoLink
        share.share(receipt, way) -> Shared.Opened
        else -> Shared.Failed
    }
}
