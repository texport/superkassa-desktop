package kz.mybrain.superkassa.presentation.login

import kz.mybrain.superkassa.KassaExtremes
import kz.mybrain.superkassa.KassaExtremes.Case
import kz.mybrain.superkassa.KassaProbe
import kz.mybrain.superkassa.eachWindow
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.LoginScene
import kz.mybrain.superkassa.kassa.inlineMain
import kz.mybrain.superkassa.presentation.strings.common.stringsOf
import kz.mybrain.superkassa.wholeOnScreen
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Вход при пятидесяти кассах с общим началом названия.
 *
 * Под название кассы была одна строка, и полсотни касс «Касса торгового
 * зала…» различались только многоточием. Меряется, что название выбранной
 * кассы в списке не обрезано, а «Войти» видна целиком и не ниже цели
 * нажатия.
 *
 * Кадры — `/tmp/adaptive-kassa-login-*.png`.
 */
class LoginAdaptiveTest {

    private fun check(probe: KassaProbe, case: Case): List<String> {
        val kkms = KassaExtremes.kkms()
        val app = CoreScene.app(LoginScene.core(kkms))
        val texts = stringsOf(case.language)
        val failures = mutableListOf<String>()
        probe.show(case.look, case.language) {
            LoginScene.Door(app)
        }
        probe.frame(KassaProbe.SETTLE)
        val name = requireNotNull(kkms.first().name)
        val row = probe.part(name)
        if (row == null || probe.cut(row)) failures += "${case.tag}: название кассы обрезано"
        probe.click(name)
        probe.save("login-${case.tag}")
        val enter = probe.node(texts.login.enter)
        println("вход ${case.tag}: название ${row?.size}, «Войти» ${enter?.boundsInRoot}")
        if (enter?.wholeOnScreen(case.width, case.height) != true) failures += "${case.tag}: «Войти» не видна"
        if ((enter?.size?.height ?: 0) < TOUCH) failures += "${case.tag}: «Войти» ниже цели нажатия"
        return failures
    }

    @Test
    fun `название кассы различимо, кнопка входа видна`() {
        val failures = inlineMain { eachWindow { probe, case -> check(probe, case) } }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }

    private companion object {
        const val TOUCH = 48
    }
}
