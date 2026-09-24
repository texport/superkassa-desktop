package kz.mybrain.superkassa.presentation.settings.workplace

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kz.mybrain.superkassa.KassaScene
import kz.mybrain.superkassa.designsystem.theme.size.Spacing
import kz.mybrain.superkassa.domain.kassa.model.sale.DomainKind
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.presentation.kassa.sale.SaleContent
import kz.mybrain.superkassa.presentation.kassa.sale.SaleUiState
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
            SaleContent(sellingState(DomainKind.Trading))
        }
        val taxi = KassaScene.shot("domain-settings-sale-taxi", height = TALL) {
            SaleContent(sellingState(DomainKind.Taxi))
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
            Card(DomainKind.Trading)
        }
        val parking = KassaScene.shot("domain-settings-card-parking", width = CARD, height = CARD_TALL) {
            Card(DomainKind.Parking)
        }

        assertTrue(trading.isNotEmpty() && parking.isNotEmpty())
        assertTrue(
            !trading.contentEquals(parking),
            "карточка не показывает, чего выбранная отрасль потребует от кассира"
        )
    }

    /** Карточка настройки — та же, что стоит на вкладке «Касса». */
    @Composable
    private fun Card(kind: DomainKind) {
        val workplace = WorkplaceSettingsUiState(kkmId = KassaScene.kkm().kkmId, domainCode = kind.code)
        Surface(Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(Spacing.fieldGap)) {
                TradeDomainCard(workplace, object : WorkplaceSettingsActions {})
            }
        }
    }

    /** Продажа с открытой сменой в выбранной отрасли. */
    private fun sellingState(kind: DomainKind) =
        SaleUiState(kkm = CoreScene.kkm(), signedIn = true, shiftOpen = true, domainKind = kind)

    private companion object {
        /** Высота кадра, на которую входит кассовая колонка целиком. */
        const val TALL = 1120

        /** Кадр одной карточки настроек. */
        const val CARD = 620
        const val CARD_TALL = 320
    }
}
