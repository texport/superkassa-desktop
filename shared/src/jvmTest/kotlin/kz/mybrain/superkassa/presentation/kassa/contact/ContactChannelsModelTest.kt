package kz.mybrain.superkassa.presentation.kassa.contact

import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import io.github.texport.superkassa.core.domain.api.model.settings.DeliveryChannelSettings
import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings
import io.github.texport.superkassa.core.domain.api.model.settings.EmailProviderSettings
import io.github.texport.superkassa.core.domain.api.model.settings.SmsProviderSettings
import io.github.texport.superkassa.core.domain.api.model.settings.TelegramProviderSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.domain.kassa.model.BuyerContact
import kz.mybrain.superkassa.domain.kassa.model.ContactChannels
import kz.mybrain.superkassa.domain.kassa.model.ContactKind
import kz.mybrain.superkassa.domain.kassa.port.FixedDeliverySetup
import kz.mybrain.superkassa.domain.kassa.port.KassaPorts
import kz.mybrain.superkassa.kassa.CoreScene
import kz.mybrain.superkassa.presentation.kassa.refund.ReturnsScene
import kz.mybrain.superkassa.presentation.kassa.refund.ReturnsViewModel
import kz.mybrain.superkassa.presentation.kassa.refund.returnsModel
import kz.mybrain.superkassa.presentation.kassa.sale.SaleScene
import kz.mybrain.superkassa.presentation.kassa.sale.saleModel
import kz.mybrain.superkassa.strings.api.Language
import kz.mybrain.superkassa.strings.api.textsOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Выбор контакта покупателя в продаже и возврате — на трёх состояниях
 * доставки: не настроено ничего, настроено не всё, настроено всё.
 *
 * Модели берут каналы из настроек доставки через свой сценарий, а не из
 * экрана; экран только показывает: погашенный вид с пометкой «не настроен»
 * или одну строку вместо выбора.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ContactChannelsModelTest {
    private val texts = textsOf(Language.Ru).kassa.contact

    @BeforeTest
    fun inlineMain() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun restoreMain() = Dispatchers.resetMain()

    private fun ports(settings: DeliverySettings?) = KassaPorts(FixedDeliverySetup(settings))

    private fun sale(settings: DeliverySettings?) =
        saleModel(CoreScene.app(SaleScene.core(), SaleScene.signedIn(), ports = ports(settings))).also { it.visit() }

    private fun returns(settings: DeliverySettings?): ReturnsViewModel {
        val app = CoreScene.app(ReturnsScene.core(listOf(BASIS)), SaleScene.signedIn(), ports = ports(settings))
        return returnsModel(app).also { it.visit() }.also { it.choose(BASIS) }
    }

    @Test
    fun `ничего не настроено — продажа и возврат не выбирают ничего`() {
        val sale = sale(null)
        sale.form.contact.kind(ContactKind.Phone)
        val refund = returns(DeliverySettings())
        refund.refund.contactKind(ContactKind.Email)

        assertTrue(sale.state.value.channels.none)
        assertEquals(ContactKind.None, sale.state.value.form.contact.kind)
        assertTrue(refund.state.value.channels.none)
        assertEquals(ContactKind.None, refund.state.value.refund?.contact?.kind)
    }

    @Test
    fun `настроено не всё — выбирается только настроенное`() {
        val sale = sale(SMS_ONLY)
        sale.form.contact.kind(ContactKind.Email)
        assertEquals(ContactKind.None, sale.state.value.form.contact.kind, "выбралась почта без канала")
        sale.form.contact.kind(ContactKind.Phone)
        assertEquals(ContactKind.Phone, sale.state.value.form.contact.kind)

        val refund = returns(SMS_ONLY)
        refund.refund.contactKind(ContactKind.Telegram)
        assertEquals(ContactKind.None, refund.state.value.refund?.contact?.kind, "выбрался Telegram без канала")
        refund.refund.contactKind(ContactKind.Phone)
        assertEquals(ContactKind.Phone, refund.state.value.refund?.contact?.kind)
    }

    @Test
    fun `настроено всё — выбирается любой вид, а по умолчанию «не отправлять»`() {
        val sale = sale(ALL)
        assertEquals(ContactKind.None, sale.state.value.form.contact.kind)
        assertEquals(setOf(ContactKind.Phone, ContactKind.Email, ContactKind.Telegram), sale.state.value.channels.ready)
        sale.form.contact.kind(ContactKind.Telegram)
        assertEquals(ContactKind.Telegram, sale.state.value.form.contact.kind)

        val refund = returns(ALL)
        assertEquals(ContactKind.None, refund.state.value.refund?.contact?.kind)
        refund.refund.contactKind(ContactKind.Email)
        assertEquals(ContactKind.Email, refund.state.value.refund?.contact?.kind)
    }

    @Test
    fun `канал пропал из настроек — выбранный вид становится «не отправлять»`() {
        val sale = sale(ALL)
        sale.form.contact.kind(ContactKind.Email)
        val kept = sale.state.value.withChannels(ContactChannels.of(SMS_ONLY))

        assertEquals(ContactKind.None, kept.form.contact.kind)
    }

    @Test
    fun `экран на трёх состояниях`() {
        val none = shown(ContactChannels())
        assertTrue(none.texts.any { texts.unavailable in it }, "без каналов нет строки-подсказки")
        assertTrue(none.segments.isEmpty(), "без каналов стоит выбор")

        val some = shown(ContactChannels(setOf(ContactKind.Phone)))
        assertTrue(some.texts.any { it.startsWith(texts.notConfigured) && "Почта" in it && "Telegram" in it })
        val expected = mapOf("Не отправлять" to true, "Телефон" to true, "Почта" to false, "Telegram" to false)
        assertEquals(expected, some.segments)

        val all = shown(ContactChannels(setOf(ContactKind.Phone, ContactKind.Email, ContactKind.Telegram)))
        assertFalse(all.texts.any { it.startsWith(texts.notConfigured) }, "всё настроено, а пометка стоит")
        assertTrue(all.segments.size == ContactKind.entries.size && all.segments.values.all { it })
    }

    /** Что видно: надписи и сегменты выбора — подпись и можно ли нажать. */
    private class Shown(val texts: List<String>, val segments: Map<String, Boolean>)

    private fun shown(channels: ContactChannels) = RenderProbe(width = 720, height = 400) {
        BuyerContactFields(BuyerContact(), channels, onKind = {}, onText = {})
    }.use { probe ->
        repeat(SETTLE) { probe.frame() }
        probe.nodes { nodes ->
            val segments = nodes.filter { it.config.getOrNull(SemanticsProperties.Role) != null }
                .associate { it.words() to !it.config.contains(SemanticsProperties.Disabled) }
            Shown(nodes.map { it.words() }.filter { it.isNotEmpty() }, segments)
        }
    }

    /** Надпись узла вместе с надписями его потомков: у сегмента она лежит в потомке. */
    private fun SemanticsNode.words(): String {
        val own = config.getOrNull(SemanticsProperties.Text)?.joinToString(" ") { it.text }
        return (listOfNotNull(own) + children.map { it.words() }).filter { it.isNotEmpty() }.joinToString(" ")
    }

    private companion object {
        const val SETTLE = 5
        val BASIS = ReturnsScene.sale(42, 90_000)

        val SMS_ONLY = DeliverySettings(
            channels = listOf(DeliveryChannelSettings("SMS")),
            sms = SmsProviderSettings(providerUrl = "https://sms.example.kz/send?to={phone}")
        )

        val ALL = SMS_ONLY.copy(
            channels = listOf("SMS", "EMAIL", "TELEGRAM").map { DeliveryChannelSettings(it) },
            email = EmailProviderSettings(host = "smtp.example.kz"),
            telegram = TelegramProviderSettings(botToken = "bot")
        )
    }
}
