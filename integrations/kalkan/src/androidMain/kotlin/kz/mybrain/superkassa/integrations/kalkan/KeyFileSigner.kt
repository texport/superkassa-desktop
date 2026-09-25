package kz.mybrain.superkassa.integrations.kalkan

import kz.gov.pki.kalkan.jce.provider.cms.CMSProcessableByteArray
import kz.gov.pki.kalkan.jce.provider.cms.CMSSignedDataGenerator
import java.security.cert.CertStore
import java.security.cert.CollectionCertStoreParameters

/**
 * Подпись файлом ключа ЭЦП НУЦ РК (PKCS#12) — ГОСТ 34.10-2015, ГОСТ 34.10-2004 и RSA.
 *
 * Подпись — CMS без вложенного содержимого, с сертификатом подписавшего:
 * так же подписывает NCALayer (`encapsulate: false`), и кабинет проверяет
 * её, подставляя содержимое сам.
 *
 * Ключ открывается на время одной подписи и нигде не остаётся; пароль
 * здесь не затирается — массив принадлежит вызывающему, и затирает его он.
 */
object KeyFileSigner {

    /**
     * Подписывает [content] ключом из [keyFile].
     *
     * @param keyFile содержимое файла `.p12`.
     * @param password пароль к файлу.
     * @param content что подписывается — байты, а не base64.
     * @return CMS в DER.
     * @throws KalkanRefusal пароль не подошёл, файл не ключ, ключ не для подписи или просрочен.
     */
    fun sign(keyFile: ByteArray, password: CharArray, content: ByteArray): ByteArray {
        Kalkan.ensureRegistered()
        val key = SigningKey.open(keyFile, password)
        return try {
            val generator = CMSSignedDataGenerator()
            generator.addSigner(key.privateKey, key.certificate, key.digest)
            val chain = CollectionCertStoreParameters(listOf(key.certificate))
            generator.addCertificatesAndCRLs(CertStore.getInstance(COLLECTION, chain, Kalkan.NAME))
            generator.generate(CMSProcessableByteArray(content), false, Kalkan.NAME).encoded
        } catch (failure: java.security.GeneralSecurityException) {
            throw KalkanRefusal(KalkanReason.Unreadable, failure)
        }
    }

    private const val COLLECTION = "Collection"
}
