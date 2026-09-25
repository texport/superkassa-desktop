package kz.mybrain.superkassa.integrations.kalkan

import kz.gov.pki.kalkan.jce.provider.KalkanProvider
import java.security.Security

/**
 * Провайдер Kalkan в списке провайдеров процесса — один раз на процесс.
 *
 * Добавляется в конец списка: на TLS, хеши и RSA остальных частей
 * приложения он не влияет — к нему обращаются по имени [NAME].
 */
internal object Kalkan {
    val NAME: String = KalkanProvider.PROVIDER_NAME

    @Synchronized
    fun ensureRegistered() {
        if (Security.getProvider(NAME) == null) Security.addProvider(KalkanProvider())
    }
}
