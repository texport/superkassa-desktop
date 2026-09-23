package kz.mybrain.superkassa.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.sale.DomainKind
import kz.mybrain.superkassa.desktop.ui.sale.SaleScreen
import kz.mybrain.superkassa.desktop.ui.settings.TradeDomainCard
import kz.mybrain.superkassa.desktop.ui.theme.Spacing
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Снимки отрасли: экран продажи и карточка настройки.
 *
 * Смотрит человек, и смотрит на одно: у кассы в торговле на продаже
 * нет ни выбора отрасли, ни её полей, а у кассы такси стоят номер
 * машины, тариф и признак заказа — и больше ничего чужого.
 *
 * Снимки — `/tmp/kassa-domain-settings-*.png`.
 */
class DomainSettingShots {

    @Test
    fun `экран продажи у кассы в торговле и у кассы такси`() {
        val trading = KassaScene.shot("domain-settings-sale-trading", height = TALL) {
            SaleScreen(sellingIn(DomainKind.Trading, "domain-shot-trading"))
        }
        val taxi = KassaScene.shot("domain-settings-sale-taxi", height = TALL) {
            SaleScreen(sellingIn(DomainKind.Taxi, "domain-shot-taxi"))
        }

        assertTrue(trading.isNotEmpty() && taxi.isNotEmpty())
        assertTrue(
            !trading.contentEquals(taxi),
            "экран продажи у торговли и у такси неотличим: отраслевых полей не видно"
        )
    }

    @Test
    fun `карточка настройки называет отрасль и её реквизиты`() {
        val trading = KassaScene.shot("domain-settings-card-trading", width = CARD, height = CARD_TALL) {
            Card(sellingIn(DomainKind.Trading, "domain-card-trading"))
        }
        val parking = KassaScene.shot("domain-settings-card-parking", width = CARD, height = CARD_TALL) {
            Card(sellingIn(DomainKind.Parking, "domain-card-parking"))
        }

        assertTrue(trading.isNotEmpty() && parking.isNotEmpty())
        assertTrue(
            !trading.contentEquals(parking),
            "карточка не показывает, чего выбранная отрасль потребует от кассира"
        )
    }

    /** Карточка настройки — та же, что стоит на вкладке «Касса». */
    @Composable
    private fun Card(session: Session) {
        Surface(Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(Spacing.screen)) { TradeDomainCard(session) }
        }
    }

    /** Рабочее место с открытой сменой и выбранной отраслью. */
    private fun sellingIn(kind: DomainKind, folder: String): Session =
        KassaScene.session(folder, shift = KassaScene.openShift()).also { it.chooseDomain(kind) }

    private companion object {
        /** Высота кадра, на которую входит кассовая колонка целиком. */
        const val TALL = 1120

        /** Кадр одной карточки настроек. */
        const val CARD = 620
        const val CARD_TALL = 320
    }
}
