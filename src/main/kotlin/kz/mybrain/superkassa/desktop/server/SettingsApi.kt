package kz.mybrain.superkassa.desktop.server

import io.ktor.http.HttpMethod
import kotlinx.serialization.Serializable

/**
 * Настройки самой кассы: налоги и распорядок смены.
 *
 * Узел принимает их только в режиме программирования, при закрытой смене
 * и пустой очереди отправки: налоговый режим меняет то, чем облагаются
 * чеки, и менять его посреди смены значит получить смену с двумя разными
 * налогами в одном Z-отчёте.
 */
@Serializable
data class TaxSettings(val taxRegime: String, val defaultVatGroup: String)

@Serializable
data class AutoCashout(val autoCashout: Boolean)

suspend fun ServerClient.updateTaxSettings(kkmId: String, settings: TaxSettings, pin: String): Kkm =
    request(HttpMethod.Put, "/kkm/$kkmId/settings/tax", settings, pin)

suspend fun ServerClient.updateAutoCashout(kkmId: String, value: Boolean, pin: String): Kkm =
    request(HttpMethod.Put, "/kkm/$kkmId/settings/autocashout", AutoCashout(value), pin)
