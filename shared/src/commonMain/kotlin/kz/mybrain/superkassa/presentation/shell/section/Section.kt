package kz.mybrain.superkassa.presentation.shell.section

import androidx.compose.ui.graphics.vector.ImageVector
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.navigation.section.CabinetKey
import kz.mybrain.superkassa.navigation.section.CashKey
import kz.mybrain.superkassa.navigation.section.DashboardKey
import kz.mybrain.superkassa.navigation.section.HistoryKey
import kz.mybrain.superkassa.navigation.section.QueueKey
import kz.mybrain.superkassa.navigation.section.RegisterKey
import kz.mybrain.superkassa.navigation.section.ReturnsKey
import kz.mybrain.superkassa.navigation.section.SaleKey
import kz.mybrain.superkassa.navigation.section.SectionKey
import kz.mybrain.superkassa.navigation.section.SettingsKey
import kz.mybrain.superkassa.navigation.section.UsersKey
import kz.mybrain.superkassa.presentation.shell.AreaPorts
import kz.mybrain.superkassa.strings.api.common.SectionTexts

/**
 * Разделы кассы.
 *
 * Порядок повторяет приложение для iOS: кассир, пересевший за компьютер,
 * ищет продажу там же, где привык. Название берётся из словаря текущего
 * языка, значок — из общего набора, ключ — из навигации окна.
 */
internal enum class Section(
    val key: SectionKey,
    val icon: ImageVector,
    val title: (SectionTexts) -> String,
    /** Раздел, на который узел отвечает только администратору. */
    val adminOnly: Boolean = false
) {
    Dashboard(DashboardKey, AppIcons.dashboard, { it.dashboard }),
    Sale(SaleKey, AppIcons.sale, { it.sale }),
    Returns(ReturnsKey, AppIcons.returns, { it.returns }),
    Cash(CashKey, AppIcons.cash, { it.cash }),
    History(HistoryKey, AppIcons.history, { it.history }),
    Queue(QueueKey, AppIcons.queue, { it.queue }, adminOnly = true),
    Users(UsersKey, AppIcons.users, { it.users }, adminOnly = true),
    Register(RegisterKey, AppIcons.newKkm, { it.register }, adminOnly = true),
    Cabinet(CabinetKey, AppIcons.cabinet, { it.cabinet }, adminOnly = true),
    Settings(SettingsKey, AppIcons.settings, { it.settings }, adminOnly = true);

    companion object {
        /** Раздел по его ключу навигации. */
        fun of(key: SectionKey): Section = entries.first { it.key == key }
    }
}

/**
 * Разделы, которые видит вошедший.
 *
 * Кассиру — только его: очередь, кассиров и настройки касса отдаёт
 * администратору. И только те, что собраны на этой платформе: кабинет
 * входит подписью ЭЦП, а мастер подключения — через кабинет или вручную;
 * раздел без своих портов открывался бы отказом, который ничему не учит.
 */
internal fun sectionsFor(isAdmin: Boolean, areas: AreaPorts): List<Section> =
    Section.entries.filter { (isAdmin || !it.adminOnly) && it.assembledWith(areas) }

private fun Section.assembledWith(areas: AreaPorts): Boolean = when (this) {
    Section.Cabinet -> areas.cabinet != null
    Section.Register -> areas.setup != null
    else -> true
}
