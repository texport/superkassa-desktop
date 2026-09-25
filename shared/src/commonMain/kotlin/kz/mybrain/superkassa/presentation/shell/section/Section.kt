package kz.mybrain.superkassa.presentation.shell.section

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.graphics.vector.ImageVector
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.presentation.shell.AreaPorts
import kz.mybrain.superkassa.strings.api.common.SectionStrings

/**
 * Разделы кассы.
 *
 * Порядок повторяет приложение для iOS: кассир, пересевший за компьютер,
 * ищет продажу там же, где привык. Название берётся из словаря текущего
 * языка, значок — из общего набора.
 */
internal enum class Section(
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

/**
 * Разделы, по которым прошёл кассир, — путь для жеста «назад».
 *
 * Главный экран лежит в основании всегда: «назад» из любого раздела
 * в конце концов приводит на него, а с него — сворачивает приложение.
 * Раздел, открытый второй раз, встаёт наверх, а не повторяется: иначе
 * «назад» после продажи, журнала и снова продажи водил бы кругами.
 *
 * @property sections пройденные разделы; последний — открытый сейчас.
 */
internal data class SectionTrail(val sections: List<Section> = listOf(Section.Dashboard)) {

    /** Открытый сейчас раздел. */
    val current: Section get() = sections.last()

    /** Есть куда шагнуть назад, не выходя из приложения. */
    val canGoBack: Boolean get() = sections.size > 1

    /** Кассир открыл [section]; главный экран начинает путь заново. */
    fun open(section: Section): SectionTrail = when (section) {
        Section.Dashboard -> SectionTrail()
        else -> SectionTrail(sections.filterNot { it == section } + section)
    }

    /** Шаг назад: предыдущий раздел пути. */
    fun back(): SectionTrail = if (canGoBack) SectionTrail(sections.dropLast(1)) else this

    /** Путь без разделов, которых вошедшему больше не видно, — например, после смены кассира. */
    fun within(shown: List<Section>): SectionTrail =
        SectionTrail(listOf(Section.Dashboard) + sections.filter { it != Section.Dashboard && it in shown })
}

/** Путь по разделам сохраняется именами разделов: так он переживает и поворот, и выгрузку процесса. */
internal val SectionTrailSaver: Saver<SectionTrail, Any> = listSaver(
    save = { trail -> trail.sections.map { it.name } },
    restore = { names -> SectionTrail(names.map(Section::valueOf)) }
)
