package kz.mybrain.superkassa.presentation.settings.workplace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.io.files.Path
import kz.mybrain.superkassa.data.local.workplace.Preferences
import kz.mybrain.superkassa.domain.kassa.model.decimal
import kz.mybrain.superkassa.domain.kassa.model.entry.PositionDraft
import kz.mybrain.superkassa.domain.kassa.model.sale.DomainInput
import kz.mybrain.superkassa.domain.kassa.model.sale.DomainKind
import kz.mybrain.superkassa.domain.kassa.model.sale.Position
import kz.mybrain.superkassa.domain.kassa.model.sale.SaleForm
import kz.mybrain.superkassa.domain.kassa.model.tenge
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.kassa.SaleScene
import kz.mybrain.superkassa.kassa.SaleScene.receipts
import kz.mybrain.superkassa.kassa.app
import kz.mybrain.superkassa.presentation.kassa.sale.saleModel
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Отрасль как настройка кассы.
 *
 * Прежде вид отрасли выбирался в каждом чеке, и кассир магазина
 * разбирался с полями такси. Теперь отрасль у кассы одна: она помнится
 * на рабочем месте и обязана доходить до запроса — протокол требует её
 * у каждого чека.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DomainSettingTest {

    @Test
    fun `по умолчанию касса работает в торговле`() {
        assertEquals(DomainKind.Trading, domainOf(Preferences(freshHome()), TAXI_KKM))
    }

    /**
     * За одной машиной работают несколько касс, и отрасли у них разные:
     * общая на рабочее место настройка переносила такси на магазинную
     * кассу при переключении.
     */
    @Test
    fun `отрасль помнится за каждой кассой отдельно`() {
        val home = freshHome()

        Preferences(home).chooseDomain(TAXI_KKM, DomainKind.Taxi.code)

        val saved = Preferences(home)
        assertEquals(DomainKind.Taxi, domainOf(saved, TAXI_KKM), "отрасль забылась")
        assertEquals(DomainKind.Trading, domainOf(saved, SHOP_KKM), "отрасль такси перешла на другую кассу")

        Preferences(home).chooseDomain(TAXI_KKM, DomainKind.Trading.code)
        assertEquals(DomainKind.Trading, domainOf(Preferences(home), TAXI_KKM))
    }

    /**
     * Настройка доходит до чека — до того самого, который уходит кассе,
     * а не до промежуточного снимка экрана. Отрасль ставят настройки,
     * а читает продажа через память рабочего места.
     */
    @Test
    fun `отрасль кассы уходит в чек вместе с её реквизитами`() {
        val preferences = Preferences(freshHome())
        preferences.chooseDomain(CoreScene.kkm().kkmId, DomainKind.Taxi.code)
        val core = SaleScene.core()
        val receipts = core.receipts()
        val app = CoreScene.app(core, SaleScene.signedIn(), memory = preferences)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val model = saleModel(app.services, app.areas.kassa)
            model.form.domain(DomainInput(carNumber = "777ABC", isOrder = true, currentFee = "350"))
            model.entry.editDraft(PositionDraft(name = POSITION.name, price = "1500", measureUnitCode = "796"))
            model.entry.addDraft()
            model.issue()
        } finally {
            Dispatchers.resetMain()
        }

        val domain = assertNotNull(receipts.commands.single().domain)
        assertEquals("DOMAIN_TAXI", domain.type, "в чеке нет вида отрасли")
        assertEquals("777ABC", domain.taxi?.carNumber, "в чеке нет номера машины")
        assertNull(domain.parking, "в чеке оказался второй подблок")
    }

    /** Чек, пробитый до смены настройки, не уносит её с собой в следующий. */
    @Test
    fun `набранные реквизиты забываются вместе с чеком`() {
        val form = SaleForm(domain = DomainInput(carNumber = "777ABC", currentFee = "350"))

        assertEquals(DomainInput(), form.next().domain, "реквизиты покупателя ушли в следующий чек")
    }

    /** Свой каталог настроек у каждой проверки: общий затёр бы рабочую кассу. */
    private fun freshHome(): Path = Path(Files.createTempDirectory("superkassa-domain").toString())

    /** Отрасль кассы так, как её прочтёт продажа: код из памяти рабочего места. */
    private fun domainOf(memory: Preferences, kkmId: String): DomainKind = DomainKind.byCode(memory.domain(kkmId))

    private companion object {
        const val TAXI_KKM = "kkm-taxi"
        const val SHOP_KKM = "kkm-shop"

        val POSITION = Position(
            name = "Поездка",
            price = tenge("1500.00"),
            quantity = decimal("1"),
            vatGroup = "VAT_16",
            measureUnitCode = "796"
        )
    }
}
