package kz.mybrain.superkassa.integrations.kalkan

import kz.gov.pki.kalkan.asn1.knca.KNCAObjectIdentifiers
import kz.gov.pki.kalkan.asn1.pkcs.PKCSObjectIdentifiers
import kz.gov.pki.kalkan.asn1.x509.ExtendedKeyUsage
import kz.gov.pki.kalkan.asn1.x509.KeyPurposeId
import kz.gov.pki.kalkan.asn1.x509.X509Extensions
import kz.gov.pki.kalkan.x509.X509V3CertificateGenerator
import java.io.ByteArrayOutputStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.Date
import javax.security.auth.x500.X500Principal
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * Файлы ключей для проверок — выпущены самим Kalkan, как у НУЦ:
 * ГОСТ 34.10-2015 512 бит и RSA 2048, самоподписанные.
 */
internal object TestKeys {
    val PASSWORD = "Qwerty12".toCharArray()

    enum class Family(val keyAlgorithm: String, val size: Int, val signature: String) {
        Gost("ECGOST3410-2015", 512, KNCAObjectIdentifiers.gost3411_2015_with_gost3410_2015_512.id),
        Rsa("RSA", 2048, PKCSObjectIdentifiers.sha256WithRSAEncryption.id)
    }

    /** Файл `.p12` с одним ключом; [purpose] — расширенное назначение, `null` — не объявлено. */
    fun keyFile(
        family: Family,
        purpose: KeyPurposeId? = KeyPurposeId.id_kp_emailProtection,
        from: Duration = (-1).days
    ): Pair<ByteArray, X509Certificate> {
        Kalkan.ensureRegistered()
        val pair = KeyPairGenerator.getInstance(family.keyAlgorithm, Kalkan.NAME).apply { initialize(family.size) }
            .generateKeyPair()
        val subject = X500Principal("CN=ТЕСТОВ ТЕСТ, SERIALNUMBER=IIN123456789011, C=KZ")
        val builder = X509V3CertificateGenerator().apply {
            setSerialNumber(BigInteger.valueOf(System.nanoTime()))
            setIssuerDN(subject)
            setSubjectDN(subject)
            setNotBefore(Date(System.currentTimeMillis() + from.inWholeMilliseconds))
            setNotAfter(Date(System.currentTimeMillis() + from.inWholeMilliseconds + 365.days.inWholeMilliseconds))
            setPublicKey(pair.public)
            setSignatureAlgorithm(family.signature)
            purpose?.let { addExtension(X509Extensions.ExtendedKeyUsage, false, ExtendedKeyUsage(it)) }
        }
        val certificate = builder.generate(pair.private, Kalkan.NAME)
        val store = KeyStore.getInstance("PKCS12", Kalkan.NAME).apply { load(null, null) }
        store.setKeyEntry("key", pair.private, PASSWORD, arrayOf(certificate))
        val file = ByteArrayOutputStream().also { store.store(it, PASSWORD) }.toByteArray()
        return file to certificate
    }
}
