package kz.mybrain.superkassa.desktop.ui

import androidx.compose.ui.graphics.vector.ImageVector
import kz.mybrain.superkassa.desktop.ui.strings.SectionStrings
import kz.mybrain.superkassa.desktop.ui.theme.AppIcons

/**
 * Разделы кассы.
 *
 * Порядок повторяет приложение для iOS: кассир, пересевший за компьютер,
 * ищет продажу там же, где привык. Название берётся из словаря текущего
 * языка, значок — из общего набора.
 */
enum class Section(
    val icon: ImageVector,
    val title: (SectionStrings) -> String,
    /** Раздел, на который узел отвечает только администратору. */
    val adminOnly: Boolean = false
) {
    Dashboard(AppIcons.dashboard, { it.dashboard }),
    Sale(AppIcons.sale, { it.sale }),
    Returns(AppIcons.returns, { it.returns }),
    Cash(AppIcons.cash, { it.cash }),
    History(AppIcons.history, { it.history }),
    Queue(AppIcons.queue, { it.queue }, adminOnly = true),
    Users(AppIcons.users, { it.users }, adminOnly = true),
    Register(AppIcons.newKkm, { it.register }, adminOnly = true),
    Cabinet(AppIcons.cabinet, { it.cabinet }, adminOnly = true),
    Settings(AppIcons.settings, { it.settings }, adminOnly = true)
}
