package kz.mybrain.superkassa.domain.kassa.usecase

import io.github.texport.superkassa.core.presentation.api.model.ofd.NomenclatureLookupRequest
import io.github.texport.superkassa.core.presentation.api.model.ofd.NomenclatureLookupResponse
import kz.mybrain.superkassa.domain.kassa.model.Answer
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.askSeated
import kz.mybrain.superkassa.domain.signin.port.SignedKkm

/** Ищет товар по штрихкоду в справочнике БФД через кассу, за которой работают. */
class LookupGoods(private val kassa: Kassa, private val signed: SignedKkm) {
    suspend operator fun invoke(barcode: String): Answer<NomenclatureLookupResponse> =
        kassa.askSeated(signed) { api, seat ->
            api.lookupNomenclature(seat.pin, NomenclatureLookupRequest(seat.kkmId, barcode))
        }
}
