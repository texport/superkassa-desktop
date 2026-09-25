package kz.mybrain.superkassa.presentation.shell.section

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import kz.mybrain.superkassa.designsystem.theme.icon.AppIcons
import kz.mybrain.superkassa.navigation.section.CabinetKey
import kz.mybrain.superkassa.navigation.section.KkmsKey
import kz.mybrain.superkassa.navigation.section.RegisterKey
import kz.mybrain.superkassa.navigation.section.SettingsKey
import kz.mybrain.superkassa.presentation.shell.AreaPorts
import kz.mybrain.superkassa.strings.api.common.SectionTexts

/**
 * Разделы окна до входа.
 *
 * С них начинается всё: выбрать кассу и войти, завести новую, войти
 * в кабинет БФД, настроить рабочее место. Прежде это был экран входа
 * с кнопками-дверями, за каждой — своя страница со своей стрелкой;
 * теперь окно до входа устроено как рабочее — та же навигация Material 3,
 * а «назад» из любого раздела ведёт к кассам. Разделы, общие с рабочим
 * окном, названы и нарисованы так же, как там.
 */
internal enum class DoorSection(
    val key: NavKey,
    override val icon: ImageVector,
    override val title: (SectionTexts) -> String
) : Destination {
    Kkms(KkmsKey, AppIcons.kkm, { it.kkms }),
    Register(RegisterKey, AppIcons.newKkm, { it.register }),
    Cabinet(CabinetKey, AppIcons.cabinet, { it.cabinet }),
    Settings(SettingsKey, AppIcons.settings, { it.settings });

    companion object {
        /** Раздел по ключу навигации; `null` — ключ шага внутри раздела. */
        fun of(key: NavKey): DoorSection? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Разделы до входа, за которыми на этой платформе что-то есть.
 *
 * Кабинет входит подписью ЭЦП, а мастер подключения собран не везде:
 * раздел без портов открывал бы пустое место.
 */
internal fun doorSectionsFor(areas: AreaPorts): List<DoorSection> = DoorSection.entries.filter {
    when (it) {
        DoorSection.Register -> areas.setup != null
        DoorSection.Cabinet -> areas.cabinet != null
        else -> true
    }
}

/** Раздел до входа, открытый в истории: последний ключ раздела в ней. */
internal fun List<NavKey>.currentDoor(): DoorSection =
    asReversed().firstNotNullOfOrNull { DoorSection.of(it) } ?: DoorSection.Kkms

/** Открыть раздел до входа из навигации: над кассами остаётся только он. */
internal fun MutableList<NavKey>.openDoor(door: DoorSection) {
    if (size > 1) subList(1, size).clear()
    if (door != DoorSection.Kkms) add(door.key)
}
