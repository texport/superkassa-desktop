package kz.mybrain.superkassa.presentation.users.signin

import kz.mybrain.superkassa.KassaExtremes
import kz.mybrain.superkassa.KassaProbe
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.LoginScene
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.label
import kz.mybrain.superkassa.presentation.theme.Look
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kz.mybrain.superkassa.wholeOnScreen
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Полоса входа на телефоне 360×800 — первый экран кассы на Android.
 *
 * В одну строку поле пина, касса, «Обновить» и «Войти» не помещались:
 * название кассы сжималось до буквы. Меряется, что поле пина и «Войти»
 * видны целиком, а название выбранной кассы не обрезано.
 * Кадр — `/tmp/adaptive-kassa-narrow-login-<язык>.png`.
 */
class SignInBarNarrowTest {

    private fun check(probe: KassaProbe, language: Language): List<String> {
        val kkms = KassaExtremes.kkms(KKMS)
        val app = CoreScene.app(LoginScene.core(kkms))
        val texts = textsOf(language).common
        probe.show(Look(), language) { LoginScene.Door(app) }
        probe.frame(KassaProbe.SETTLE)
        val name = requireNotNull(kkms.first().name)
        probe.click(name)
        probe.save("narrow-login-${language.name.lowercase()}")
        val failures = mutableListOf<String>()
        val pin = probe.node(texts.common.pin)
        if (pin?.wholeOnScreen(WIDTH, HEIGHT) != true) failures += "$language: поле пина за краем: ${pin?.boundsInRoot}"
        val enter = probe.node(texts.login.enter)
        if (enter?.wholeOnScreen(WIDTH, HEIGHT) != true) failures += "$language: «Войти» за краем"
        val chosen = probe.parts().filter { name in it.label() }.maxByOrNull { it.boundsInRoot.top }
        if (chosen == null || probe.cut(chosen)) failures += "$language: название выбранной кассы обрезано"
        return failures
    }

    @Test
    fun `на телефоне пин, касса и вход помещаются целиком`() {
        val failures = inlineMain {
            KassaProbe(WIDTH, HEIGHT).use { probe -> listOf(Language.Ru, Language.Kk).flatMap { check(probe, it) } }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    private companion object {
        const val WIDTH = 360
        const val HEIGHT = 800
        const val KKMS = 3
    }
}
