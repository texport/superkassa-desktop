package kz.mybrain.superkassa.desktop.ui

import androidx.compose.runtime.staticCompositionLocalOf
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

/**
 * Переход в другой раздел из глубины экрана.
 *
 * Раздел выбирает рельс, и до него от экрана кабинета три вложения. Кнопка
 * «Перейти к кассе» лежит в паспорте кассы, поэтому переход отдаётся через
 * окружение — так же, как язык и словари, — а не протягивается обработчиком
 * через каждый промежуточный экран.
 *
 * Значение по умолчанию ничего не делает: за пределами рабочего окна —
 * в наборах текстов и в тестах отдельного экрана — переходить некуда.
 */
val LocalSectionSwitch = staticCompositionLocalOf<(Section) -> Unit> { {} }
