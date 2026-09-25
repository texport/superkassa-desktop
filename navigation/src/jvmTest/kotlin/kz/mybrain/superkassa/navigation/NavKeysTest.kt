package kz.mybrain.superkassa.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.json.Json
import kz.mybrain.superkassa.navigation.section.KkmsKey
import kz.mybrain.superkassa.navigation.section.SectionKey
import kz.mybrain.superkassa.navigation.section.SettingsKey
import kz.mybrain.superkassa.navigation.step.SettingsSectionKey
import kz.mybrain.superkassa.navigation.step.StepKey
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
    fun `каждый раздел есть в реестре`() = everyKnown(SectionKey.serializer().descriptor)

    @Test
    fun `каждый шаг есть в реестре`() = everyKnown(StepKey.serializer().descriptor)

    /**
     * Все подтипы запечатанного ключа находятся реестром. У запечатанного
     * ключа второй элемент описания — все его подтипы; шаг с данными
     * узнаётся по отказу в недостающем поле, а не в неизвестном типе.
     */
    private fun everyKnown(sealed: SerialDescriptor) {
        sealed.getElementDescriptor(1).elementDescriptors.forEach { subtype ->
            val fields = (0 until subtype.elementsCount).joinToString("") { ",\"${subtype.getElementName(it)}\":\"x\"" }
            val restored = runCatching { restore("{\"type\":\"${subtype.serialName}\"$fields}") }
            assertNotNull(restored.getOrNull(), "${subtype.serialName} нет в реестре: ${restored.exceptionOrNull()}")
        }
    }

    @Test
    fun `ключ переживает сохранение`() {
        val saved = json.encodeToString(PolymorphicSerializer(NavKey::class), SettingsKey)
        assertEquals(SettingsKey, restore(saved))
    }

    @Test
    fun `кассы окна до входа переживают сохранение`() {
        assertEquals(KkmsKey, restore(json.encodeToString(PolymorphicSerializer(NavKey::class), KkmsKey)))
    }

    @Test
    fun `раздел настроек переживает сохранение вместе с именем`() {
        val key = SettingsSectionKey("Printing")
        assertEquals(key, restore(json.encodeToString(PolymorphicSerializer(NavKey::class), key)))
    }

    private fun restore(saved: String): NavKey = json.decodeFromString(PolymorphicSerializer(NavKey::class), saved)
}
