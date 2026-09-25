package kz.mybrain.superkassa.integrations.kalkan

import kz.gov.pki.kalkan.asn1.x509.KeyPurposeId
import kz.gov.pki.kalkan.jce.provider.cms.CMSProcessableByteArray
import kz.gov.pki.kalkan.jce.provider.cms.CMSSignedData
import kz.gov.pki.kalkan.jce.provider.cms.SignerInformation
import java.security.cert.X509Certificate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class KeyFileSignerTest {
    private val challenge = "cabinet challenge 5f1c".encodeToByteArray()

    @Test
    fun gostKeySignsDetachedCmsThatVerifiesAgainstTheContent() {
        val (file, certificate) = TestKeys.keyFile(TestKeys.Family.Gost)
        assertVerifies(KeyFileSigner.sign(file, TestKeys.PASSWORD.copyOf(), challenge), certificate)
    }

    @Test
    fun rsaKeySignsDetachedCmsThatVerifiesAgainstTheContent() {
        val (file, certificate) = TestKeys.keyFile(TestKeys.Family.Rsa)
        assertVerifies(KeyFileSigner.sign(file, TestKeys.PASSWORD.copyOf(), challenge), certificate)
    }

    @Test
    fun keyWithoutDeclaredPurposeSigns() {
        val (file, certificate) = TestKeys.keyFile(TestKeys.Family.Gost, purpose = null)
        assertVerifies(KeyFileSigner.sign(file, TestKeys.PASSWORD.copyOf(), challenge), certificate)
    }

    @Test
    fun wrongPasswordIsToldApartFromBrokenFile() {
        val (file, _) = TestKeys.keyFile(TestKeys.Family.Gost)
        assertEquals(KalkanReason.WrongPassword, refusalOf(file, "wrong".toCharArray()))
        assertEquals(KalkanReason.Unreadable, refusalOf("not a key".encodeToByteArray(), TestKeys.PASSWORD))
    }

    @Test
    fun authenticationKeyIsNotForSigning() {
        val (file, _) = TestKeys.keyFile(TestKeys.Family.Rsa, purpose = KeyPurposeId.id_kp_clientAuth)
        assertEquals(KalkanReason.NotForSigning, refusalOf(file, TestKeys.PASSWORD))
    }

    @Test
    fun certificateNotYetValidIsExpired() {
        val (file, _) = TestKeys.keyFile(TestKeys.Family.Gost, from = 2.days)
        assertEquals(KalkanReason.Expired, refusalOf(file, TestKeys.PASSWORD))
    }

    private fun refusalOf(file: ByteArray, password: CharArray): KalkanReason =
        assertFailsWith<KalkanRefusal> { KeyFileSigner.sign(file, password, challenge) }.reason

    private fun assertVerifies(cms: ByteArray, certificate: X509Certificate) {
        assertNull(CMSSignedData(cms).signedContent, "content must not be encapsulated")
        val signed = CMSSignedData(CMSProcessableByteArray(challenge), cms)
        val signer = signed.signerInfos.signers.single() as SignerInformation
        assertTrue(signer.verify(certificate, Kalkan.NAME))
    }
}
