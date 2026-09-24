package kz.mybrain.superkassa.kassa

import io.github.texport.superkassa.core.presentation.api.SuperkassaApi
import io.github.texport.superkassa.core.presentation.api.model.kkm.CashOperationResponse
import io.github.texport.superkassa.core.presentation.api.model.receipt.ReceiptResponse
import kz.mybrain.superkassa.domain.kassa.port.Kassa

/**
 * Касса, у которой ответ на первую фискальную команду теряется по дороге
 * к экрану: документ проведён, а экран о нём не узнал.
 *
 * Так выглядит неизвестный исход для модели экрана: повтор обязан уйти
 * с тем же ключом, и касса второго документа не проведёт.
 */
class LosingKassa(private val inner: Kassa) : Kassa {
    private var lose = true

    override suspend fun <T> call(request: (SuperkassaApi) -> T): T {
        val result = inner.call(request)
        if (lose && (result is ReceiptResponse || result is CashOperationResponse)) {
            lose = false
            error("answer lost")
        }
        return result
    }
}
