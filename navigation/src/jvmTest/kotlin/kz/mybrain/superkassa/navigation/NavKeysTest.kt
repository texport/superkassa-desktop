package kz.mybrain.superkassa.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.json.Json
import kz.mybrain.superkassa.navigation.section.SectionKey
import kz.mybrain.superkassa.navigation.section.SettingsKey
import kz.mybrain.superkassa.navigation.settings.SettingsSectionKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Реестр ключей: историю «назад» можно сохранить на любой платформе.
 *
 * Ключ, которого нет в реестре, Android сохранил бы отражением, а iOS —
 * нет: история терялась бы при выгрузке приложения только на одной
 * платформе. Проверка сверяет реестр со всеми подтипами ключей.
 */
class NavKeysTest {

    private val json = Json { serializersModule = NavKeys.serializersModule }

    @Test
    fun `каждый раздел есть в реестре`() {
        // У запечатанного ключа второй элемент описания — все его подтипы.
        val subtypes = SectionKey.serializer().descriptor.getElementDescriptor(1)
        val sections = subtypes.elementDescriptors.map { it.serialName }
        sections.forEach { name ->
            val restored = runCatching { restore("{\"type\":\"$name\"}") }
            assertNotNull(restored.getOrNull(), "$name нет в реестре")
        }
    }

    @Test
    fun `ключ переживает сохранение`() {
        val saved = json.encodeToString(PolymorphicSerializer(NavKey::class), SettingsKey)
        assertEquals(SettingsKey, restore(saved))
    }

    @Test
    fun `раздел настроек переживает сохранение вместе с именем`() {
        val key = SettingsSectionKey("Printing")
        assertEquals(key, restore(json.encodeToString(PolymorphicSerializer(NavKey::class), key)))
    }

    private fun restore(saved: String): NavKey = json.decodeFromString(PolymorphicSerializer(NavKey::class), saved)
}
