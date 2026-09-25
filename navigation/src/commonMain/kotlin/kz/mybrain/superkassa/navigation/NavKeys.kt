package kz.mybrain.superkassa.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.PolymorphicModuleBuilder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kz.mybrain.superkassa.navigation.section.CabinetKey
import kz.mybrain.superkassa.navigation.section.CashKey
import kz.mybrain.superkassa.navigation.section.DashboardKey
import kz.mybrain.superkassa.navigation.section.HistoryKey
import kz.mybrain.superkassa.navigation.section.QueueKey
import kz.mybrain.superkassa.navigation.section.RegisterKey
import kz.mybrain.superkassa.navigation.section.ReturnsKey
import kz.mybrain.superkassa.navigation.section.SaleKey
import kz.mybrain.superkassa.navigation.section.SettingsKey
import kz.mybrain.superkassa.navigation.section.UsersKey

/**
 * Реестр ключей экранов — для сохранения истории «назад».
 *
 * История переживает поворот экрана и выгрузку процесса: она сохраняется
 * сериализацией, а на iOS и в браузере нет отражения, которым Android
 * находит подтип ключа сам. Поэтому каждый ключ перечислен здесь; ключ,
 * которого нет в реестре, история не сохранит — это ловит проверка модуля.
 */
val NavKeys: SavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) { sections() }
    }
}

/** Разделы рабочего окна. */
private fun PolymorphicModuleBuilder<NavKey>.sections() {
    subclass(DashboardKey::class, DashboardKey.serializer())
    subclass(SaleKey::class, SaleKey.serializer())
    subclass(ReturnsKey::class, ReturnsKey.serializer())
    subclass(CashKey::class, CashKey.serializer())
    subclass(HistoryKey::class, HistoryKey.serializer())
    subclass(QueueKey::class, QueueKey.serializer())
    subclass(UsersKey::class, UsersKey.serializer())
    subclass(RegisterKey::class, RegisterKey.serializer())
    subclass(CabinetKey::class, CabinetKey.serializer())
    subclass(SettingsKey::class, SettingsKey.serializer())
}
