package kz.mybrain.superkassa.integrations.kalkan

import kz.gov.pki.kalkan.asn1.knca.KNCAObjectIdentifiers
import kz.gov.pki.kalkan.asn1.nist.NISTObjectIdentifiers
import kz.gov.pki.kalkan.asn1.pkcs.PKCSObjectIdentifiers
import java.io.IOException
import java.security.KeyStore
import java.security.PrivateKey
import java.security.UnrecoverableKeyException
import java.security.cert.CertificateException
import java.security.cert.X509Certificate

/**
 * Ключ для подписи из файла PKCS#12 и его сертификат.
 *
 * @property digest OID хеша, которым подписывает этот ключ.
 */
internal class SigningKey(val privateKey: PrivateKey, val certificate: X509Certificate, val digest: String) {

    companion object {
        /**
         * Открывает файл и берёт из него ключ для подписи.
         *
         * В файле НУЦ бывает по одному ключу, но файл может нести и несколько:
         * берётся первый, чей сертификат разрешает подпись.
         */
        fun open(keyFile: ByteArray, password: CharArray): SigningKey {
            val store = load(keyFile, password)
            val (alias, certificate) = signingEntry(store)
            checkValidity(certificate)
            val digest = DIGESTS[certificate.sigAlgOID]
            val key = store.getKey(alias, password) as? PrivateKey
            if (digest == null || key == null) throw KalkanRefusal(KalkanReason.Unreadable)
            return SigningKey(key, certificate, digest)
        }

        /** Запись ключа, чей сертификат разрешает подпись: её имя и сертификат. */
        private fun signingEntry(store: KeyStore): Pair<String, X509Certificate> {
            val entries = store.aliases().toList().filter(store::isKeyEntry)
                .mapNotNull { alias -> (store.getCertificate(alias) as? X509Certificate)?.let { alias to it } }
            return entries.firstOrNull { signs(it.second) }
                ?: throw KalkanRefusal(if (entries.isEmpty()) KalkanReason.Unreadable else KalkanReason.NotForSigning)
        }

        /**
         * Хранилище из файла. Неверный пароль Kalkan сообщает проверкой
         * целостности файла — её отличают по причине и словам ошибки.
         */
        private fun load(keyFile: ByteArray, password: CharArray): KeyStore = try {
            KeyStore.getInstance(PKCS12, Kalkan.NAME).apply { load(keyFile.inputStream(), password) }
        } catch (failure: IOException) {
            val reason = if (wrongPassword(failure)) KalkanReason.WrongPassword else KalkanReason.Unreadable
            throw KalkanRefusal(reason, failure)
        } catch (failure: CertificateException) {
            throw KalkanRefusal(KalkanReason.Unreadable, failure)
        } catch (failure: IllegalArgumentException) {
            throw KalkanRefusal(KalkanReason.Unreadable, failure)
        }

        private fun wrongPassword(failure: IOException): Boolean =
            failure.cause is UnrecoverableKeyException ||
                PASSWORD_WORDS.any { failure.message.orEmpty().contains(it, ignoreCase = true) }

        /** Подписывает ли ключ: назначение не объявлено — да; объявлено — только с назначением подписи. */
        private fun signs(certificate: X509Certificate): Boolean {
            val purposes = certificate.extendedKeyUsage ?: return true
            return SIGNING_PURPOSE in purposes
        }

        private fun checkValidity(certificate: X509Certificate) = try {
            certificate.checkValidity()
        } catch (failure: CertificateException) {
            throw KalkanRefusal(KalkanReason.Expired, failure)
        }

        private const val PKCS12 = "PKCS12"

        /** Подпись документа — расширенное назначение ключа НУЦ РК (то же спрашивает NCALayer). */
        private const val SIGNING_PURPOSE = "1.3.6.1.5.5.7.3.4"

        private val PASSWORD_WORDS = listOf("password", "mac")

        /**
         * Хеш подписи по алгоритму сертификата: ключ и выпустивший его
         * удостоверяющий центр НУЦ — одной ветки, ГОСТ или RSA.
         */
        private val DIGESTS: Map<String, String> = mapOf(
            PKCSObjectIdentifiers.sha256WithRSAEncryption.id to NISTObjectIdentifiers.id_sha256.id,
            KNCAObjectIdentifiers.gost34311_95_with_gost34310_2004.id to KNCAObjectIdentifiers.gost34311_95.id,
            KNCAObjectIdentifiers.gost3411_2015_with_gost3410_2015_512.id to KNCAObjectIdentifiers.gost3411_2015_512.id,
            KNCAObjectIdentifiers.gost3411_2015_with_gost3410_2015_256.id to KNCAObjectIdentifiers.gost3411_2015_256.id
        )
    }
}
