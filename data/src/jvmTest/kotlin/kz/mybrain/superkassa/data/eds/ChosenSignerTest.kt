package kz.mybrain.superkassa.data.eds

import kotlinx.coroutines.test.runTest
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignDesk
import kz.mybrain.superkassa.domain.cabinet.model.signature.SignMethod
import kz.mybrain.superkassa.domain.cabinet.port.Signer
import kotlin.test.Test
import kotlin.test.assertEquals

class ChosenSignerTest {
    private val byEgov = named("egov")
    private val byFile = named("file")
    private val chosen = ChosenSigner(
        linkedMapOf(SignMethod.EgovMobile to byEgov, SignMethod.KeyFile to byFile),
        SignDesk()
    )

    @Test
    fun firstMethodIsTheDefault() = runTest {
        assertEquals(listOf(SignMethod.EgovMobile, SignMethod.KeyFile), chosen.methods)
        assertEquals(SignMethod.EgovMobile, chosen.method.value)
        assertEquals("egov:AA==", chosen.sign("AA=="))
    }

    @Test
    fun signsWithTheChosenMethod() = runTest {
        chosen.choose(SignMethod.KeyFile)
        assertEquals("file:AA==", chosen.sign("AA=="))
    }

    @Test
    fun methodOfAnotherPlatformIsNotChosen() {
        chosen.choose(SignMethod.NcaLayer)
        assertEquals(SignMethod.EgovMobile, chosen.method.value)
    }

    private fun named(name: String) = object : Signer {
        override suspend fun sign(payload: String): String = "$name:$payload"
    }
}
