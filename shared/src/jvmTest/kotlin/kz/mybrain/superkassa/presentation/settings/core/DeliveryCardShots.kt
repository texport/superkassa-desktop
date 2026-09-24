package kz.mybrain.superkassa.presentation.settings.core

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import io.github.texport.superkassa.core.domain.api.model.settings.CoreMode
import io.github.texport.superkassa.core.domain.api.model.settings.CoreSettings
import io.github.texport.superkassa.core.domain.api.model.settings.DeliverySettings
import io.github.texport.superkassa.core.domain.api.model.settings.EmailProviderSettings
import io.github.texport.superkassa.core.domain.api.model.settings.SmsProviderSettings
import io.github.texport.superkassa.core.domain.api.model.settings.StorageSettings
import kz.mybrain.superkassa.RenderProbe
import kz.mybrain.superkassa.domain.settings.model.DeliveryField
import kz.mybrain.superkassa.presentation.common.adaptive.ContentKind
import kz.mybrain.superkassa.presentation.common.adaptive.contentWidth
import kz.mybrain.superkassa.presentation.common.list.ScrollableColumn
import kz.mybrain.superkassa.presentation.print.target.PrintTargetActions
import kz.mybrain.superkassa.presentation.print.target.PrintTargetCard
import kz.mybrain.superkassa.presentation.print.target.PrintTargetUiState
import kz.mybrain.superkassa.presentation.strings.common.Language
import kz.mybrain.superkassa.presentation.strings.settings.deliveryTexts
import kz.mybrain.superkassa.presentation.theme.Look
import kz.mybrain.superkassa.presentation.theme.TextScale
import kz.mybrain.superkassa.presentation.theme.color.Appearance
import kz.mybrain.superkassa.presentation.theme.size.Spacing
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Карточки доставки чека, настроек кассы и принтера кассы на окнах от
 * наименьшего до широкого и на планшетах: кадры `/tmp/qa-settings-*.png`.
 *
 * Состояние предельное: SMS настроен с ключом под знаком, почта — с портом
 * вне диапазона, выбранный принтер отключён, правка открыта и закрыта.
 */
class DeliveryCardShots {

    private val settings = CoreSettings(
        mode = CoreMode.DESKTOP,
        storage = StorageSettings(engine = "SQLITE", jdbcUrl = "jdbc:sqlite:superkassa.db"),
        allowChanges = true,
        delivery = DeliverySettings(
            sms = SmsProviderSettings(providerUrl = LONG_URL, apiKey = SMS_KEY),
            email = EmailProviderSettings(host = "smtp.example.kz", user = "kassa@example.kz", password = MAIL_PASSWORD)
        )
    )

    private fun shoot(name: String, width: Int, height: Int, language: Language, scale: TextScale, dark: Boolean) {
        val frozen = name.contains("frozen")
        val core = settings.copy(allowChanges = !frozen)
        val delivery = DeliveryUiState.of(core).copy(drafts = mapOf(DeliveryField.EmailPort to "99999"))
        val appearance = if (dark) Appearance.Dark else Appearance.Light
        RenderProbe(width, height, appearance, Look(textScale = scale), language) {
            Surface(Modifier.fillMaxSize()) {
                ScrollableColumn(modifier = Modifier.fillMaxSize().padding(Spacing.screen)) {
                    Column(Modifier.contentWidth(ContentKind.Reading), Arrangement.spacedBy(Spacing.roomy)) {
                        DeliveryCard(delivery, object : DeliveryActions {})
                        PrintTargetCard(GONE, object : PrintTargetActions {})
                        CoreSettingsCard(CoreSettingsUiState(settings = core), object : CoreSettingsActions {})
                    }
                }
            }
        }.use { probe ->
            repeat(SETTLE) { probe.frame() }
            val texts = deliveryTexts(language)
            val seen = probe.nodes().joinToString(" ") { it.toString() }
            assertTrue(seen.contains(texts.title), "$name: карточки доставки нет")
            assertTrue(!seen.contains(SMS_KEY) && !seen.contains(MAIL_PASSWORD), "$name: ключ на экране")
            File("/tmp/qa-settings-$name.png").writeBytes(probe.frame())
        }
    }

    @Test
    fun `карточки на всех окнах`() {
        SIZES.forEach { (width, height) ->
            shoot("delivery-${width}x$height-ru", width, height, Language.Ru, TextScale.Normal, dark = false)
        }
        shoot("delivery-960x640-kk-larger", 960, 640, Language.Kk, TextScale.Larger, dark = false)
        shoot("delivery-800x1280-en-larger-dark", 800, 1280, Language.En, TextScale.Larger, dark = true)
        shoot("delivery-frozen-1180x820-ru-dark", 1180, 820, Language.Ru, TextScale.Normal, dark = true)
    }

    private companion object {
        val SIZES = listOf(960 to 640, 1180 to 820, 1920 to 1080, 2560 to 1080, 800 to 1280, 1280 to 800)
        val GONE = PrintTargetUiState(
            kkmId = "kkm-1",
            printers = listOf("Конторский A4"),
            printersRead = true,
            printer = "Чековый у кассы"
        )
        const val SETTLE = 20
        const val SMS_KEY = "sms-key-5d21"
        const val LONG_URL = "https://sms-gateway.example.kz/api/v2/messages/send"
        const val MAIL_PASSWORD = "mail-pass-8c04"
    }
}
