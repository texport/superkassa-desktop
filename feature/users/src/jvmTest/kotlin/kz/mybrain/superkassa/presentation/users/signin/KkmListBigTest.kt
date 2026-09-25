package kz.mybrain.superkassa.presentation.users.signin

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.renderMillis
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Список касс на входе с двумя тысячами строк: рисуется быстро и прокручивается.
 */
class KkmListBigTest {

    @Composable
    private fun Logins(count: Int) {
        val all = kkms(count)
        KkmList(LoginUiState(kkms = all, answered = true, listRead = true, pickedId = all.first().kkmId)) {}
    }

    private fun kkms(count: Int) = (1..count).map {
        CoreScene.kkm(id = "kkm-$it", kgd = "%012d".format(it), name = null).let { kkm ->
            val org = kkm.ofdServiceInfo?.copy(orgAddress = "Алматы, Абая $it")
            kkm.copy(factoryNumber = "SK-$it", ofdServiceInfo = org)
        }
    }

    @Test
    fun `список касс на входе держит две тысячи строк`() {
        renderMillis { Logins(SMALL) }
        val small = renderMillis { Logins(SMALL) }
        val large = renderMillis { Logins(LARGE) }
        println("вход: $SMALL касс — $small мс, $LARGE касс — $large мс")
        assertTrue(large < BUDGET, "$LARGE касс рисуются $large мс")
        assertTrue(large < small * FACTOR + SLACK, "рост отрисовки с длиной списка: $small → $large мс")
    }

    @Test
    fun `список касс на входе прокручивается`() {
        RenderProbe { Logins(LARGE) }.use { probe ->
            val before = probe.frame()
            probe.wheel(at = Offset(400f, 400f), ticks = 6f)
            assertTrue(probe.changedFrom(before), "картинка списка не изменилась после прокрутки")
        }
    }

    private companion object {
        const val SMALL = 5

        /** Сколько касс будет у сети, ради которой кабинет и делается. */
        const val LARGE = 2000

        /** Сколько миллисекунд отводится на сборку и отрисовку экрана целиком. */
        const val BUDGET = 1500L

        /** Во сколько раз длинный список вправе оказаться дороже короткого. */
        const val FACTOR = 2

        /** Запас на дрожание машины при сравнении замеров. */
        const val SLACK = 150L
    }
}
