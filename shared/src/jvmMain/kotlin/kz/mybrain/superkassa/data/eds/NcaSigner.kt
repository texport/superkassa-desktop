package kz.mybrain.superkassa.data.eds

import kz.mybrain.superkassa.domain.cabinet.model.EdsProblem
import kz.mybrain.superkassa.domain.cabinet.model.EdsRefusal
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kz.mybrain.superkassa.integrations.ncalayer.DesktopNcaLayer
import kz.mybrain.superkassa.integrations.ncalayer.NcaLayer
import kz.mybrain.superkassa.integrations.ncalayer.NcaReason
import kz.mybrain.superkassa.integrations.ncalayer.NcaRefusal
import kz.mybrain.superkassa.integrations.ncalayer.NcaSettings

/**
 * Подпись ключом владельца — NCALayer на этой машине.
 *
 * Протокол, запасной модуль и доверие петле — в модуле `ncalayer`; здесь его
 * отказ становится отказом подписи предметной области: что случилось —
 * [EdsProblem], а подробность — кодом [Signer], по которому экран подбирает
 * слова на языке владельца.
 */
class NcaSigner(private val layer: NcaLayer) : Signer {

    /**
     * NCALayer по адресу [address], с ожиданием подписи [Signer.SIGN_WINDOW];
     * окно подписи — на языке [locale] (`kk`, `ru` или `en`).
     *
     * Ход обмена — в журнал подписи: адрес, модуль, состав кадров, но не
     * содержимое и не подпись.
     */
    constructor(address: String = NcaSettings().address, locale: () -> String = { NcaSettings().locale }) : this(
        DesktopNcaLayer(NcaSettings(address = address, signWindow = Signer.SIGN_WINDOW), signatureJournal(), locale)
    )

    override suspend fun sign(payload: String): String = try {
        layer.sign(payload)
    } catch (refusal: NcaRefusal) {
        throw refusal.eds()
    }
}

/** Отказ NCALayer — отказом подписи: недоступен он или подписи не дал. */
internal fun NcaRefusal.eds(): EdsRefusal = when (reason) {
    NcaReason.Unreachable -> EdsRefusal(EdsProblem.Unreachable, Signer.NO_HANDSHAKE, this)
    NcaReason.Silent -> EdsRefusal(EdsProblem.Declined, Signer.NO_ANSWER, this)
    NcaReason.WindowClosed -> EdsRefusal(EdsProblem.Declined, Signer.WINDOW_CLOSED, this)
    NcaReason.Declined -> EdsRefusal(EdsProblem.Declined, detail, this)
}
