package kz.mybrain.superkassa.presentation.shell.section

import androidx.navigation3.runtime.NavKey
import kz.mybrain.superkassa.navigation.section.SectionKey

/*
 * История «назад» окна — список ключей Navigation 3.
 *
 * Разделы — верхний уровень навигации, и по рекомендации Material 3 они
 * не копятся друг над другом: раздел, открытый из навигации окна, встаёт
 * над главным экраном, а «назад» из любого раздела ведёт на главный, с него —
 * из приложения. Шаги внутри раздела ложатся поверх его ключа той же
 * историей. Главный экран — основание истории, и его не снимает ничто.
 */

/** Раздел, открытый в истории: последний ключ раздела в ней. */
internal fun List<NavKey>.currentSection(): Section =
    lastOrNull { it is SectionKey }?.let { Section.of(it as SectionKey) } ?: Section.Dashboard

/** Открыть раздел из навигации окна: над главным экраном остаётся только он. */
internal fun MutableList<NavKey>.openSection(section: Section) {
    if (size > 1) subList(1, size).clear()
    if (section != Section.Dashboard) add(section.key)
}

/** Шаг назад; с главного экрана шагать некуда — жест уходит системе. */
internal fun MutableList<NavKey>.stepBack(): Boolean = (size > 1).also { if (it) removeAt(lastIndex) }

/**
 * Снять шаг [key] и всё над ним; нет его в истории — ничего. Основание
 * истории не снимается: оно не шаг.
 */
internal fun MutableList<NavKey>.closeStep(key: NavKey) {
    val at = lastIndexOf(key)
    if (at > 0) subList(at, size).clear()
}

/**
 * Разделы, которых вошедшему не видно, — например, после смены кассира
 * на кассира без прав администратора: история возвращается на главный экран.
 */
internal fun List<NavKey>.outside(shown: List<Section>): Boolean =
    any { it is SectionKey && Section.of(it) !in shown }
