package kz.mybrain.superkassa.presentation.cabinet.signing

import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.domain.cabinet.model.signature.KeyProblem
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignAnswer
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignRequest
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.tap
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Окно подписи: пароль к файлу ключа и QR eGov mobile.
 *
 * Неудача прошлой попытки видна в том же окне словами владельца, а пароль
 * уходит массивом и из поля исчезает.
 */
class SignRequestDialogTest {
    private val eds = textsOf(Language.Ru).cabinet.eds
    private val answers = mutableListOf<SignAnswer>()

    @Test
    fun keyWindowNamesTheFileAndThePreviousProblem() {
        val asked = SignRequest.KeyPassword("AUTH_RSA256_owner.p12", KeyProblem.NotForSigning)
        shown(asked) { probe ->
            val texts = probe.nodes().joinToString(" ") { it.text }
            assertTrue("AUTH_RSA256_owner.p12" in texts, texts)
            assertTrue(eds.keyNotForSigning in texts, texts)
            probe.tap { it.text == eds.keyOther }
            settle(probe)
        }
        assertEquals(listOf<SignAnswer>(SignAnswer.OtherFile), answers)
    }

    @Test
    fun passwordLeavesAsCharactersAndTheFieldIsCleared() {
        shown(SignRequest.KeyPassword("GOST512.p12")) { probe ->
            probe.tap { it.text == eds.keySign }
            settle(probe)
            assertTrue(answers.isEmpty(), "empty password must not be signed")
            probe.tap { it.editable }
            probe.type("Qwerty12")
            settle(probe)
            probe.tap { it.text == eds.keySign }
            settle(probe)
            val fields = probe.nodes().filter { it.editable }
            assertTrue(fields.none { '•' in it.text || "Qwerty" in it.text }, "password stays: $fields")
        }
        assertContentEquals("Qwerty12".toCharArray(), assertIs<SignAnswer.Password>(answers.single()).chars)
    }

    @Test
    fun egovWindowOffersTheAppAndCancels() {
        shown(SignRequest.EgovMobile("https://launch.egov/1", byteArrayOf(), until = null)) { probe ->
            assertTrue(probe.nodes().any { it.text == eds.egovOpen })
            probe.tap { it.text == eds.cancel }
            settle(probe)
        }
        assertEquals(listOf<SignAnswer>(SignAnswer.Cancel), answers)
    }

    /** Окно просьбы на сцене: окна Compose встают только со второго-третьего кадра. */
    private fun shown(request: SignRequest, check: (RenderProbe) -> Unit) =
        RenderProbe { SignRequestDialog(request, eds) { answers += it } }.use { probe ->
            settle(probe)
            check(probe)
        }

    private fun settle(probe: RenderProbe) = repeat(SETTLE) { probe.frame() }

    private companion object {
        const val SETTLE = 12
    }
}
