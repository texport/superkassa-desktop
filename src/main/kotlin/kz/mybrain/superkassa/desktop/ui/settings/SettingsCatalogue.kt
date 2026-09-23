package kz.mybrain.superkassa.desktop.ui.settings

import androidx.compose.runtime.Composable
import kz.mybrain.superkassa.desktop.app.Session
import kz.mybrain.superkassa.desktop.ui.strings.SettingStrings

/**
 * Два хозяйства настроек: чьё это и кто их хранит.
 *
 * Прежде все настройки стояли одним столбцом, и в нём соседствовали вещи
 * разной природы: цвет приложения на этой машине, налоговый режим, который
 * принимает узел, и адрес кабинета БФД. Владелец искал нужное глазами
 * по всему экрану, а вход в режим программирования и выход из него
 * оказались в разных местах столбца — войти удавалось, а выйти уже нет.
 *
 * Хозяйство отвечает на вопрос «где это изменится»: [Workplace] —
 * на этой машине, [Kkm] — в кассе через узел.
 *
 * Третьим стояло хозяйство кабинета БФД, и в нём лежал один адрес.
 * Адрес кабинета меняется здесь же, на этой машине, — у БФД от него
 * не меняется ничего, — так что хозяйство называло владельца неверно,
 * а вкладка открывалась карточкой в поле высотой в экран.
 */
internal enum class SettingsHousehold(val title: (SettingStrings) -> String) {
    Workplace({ it.householdWorkplace }),
    Kkm({ it.householdKkm })
}

/**
 * Разделы внутри хозяйства.
 *
 * Второй уровень после вкладок: вкладка называет владельца настройки,
 * заголовок — её предмет. Заголовка нет там, где в разделе одна карточка
 * и называть отдельно нечего.
 */
internal enum class SettingsGroup(
    val household: SettingsHousehold,
    val title: (SettingStrings) -> String?
) {
    /**
     * Вид приложения и состав кассовой колонки.
     *
     * Заголовка нет: карточка так и называется — «Оформление», — и вторая
     * строка тем же словом над ней ничего не разделяла.
     */
    Look(SettingsHousehold.Workplace, { null }),

    /** Адреса служб, с которыми говорит рабочее место: узел, кабинет, карта. */
    Addresses(SettingsHousehold.Workplace, { it.groupServices }),

    /** Программа на этой машине: сведения об узле, выпуски, журнал. */
    Program(SettingsHousehold.Workplace, { it.groupProgram }),

    /** Сама касса и её режим: заголовка нет, речь о ней и так. */
    Current(SettingsHousehold.Kkm, { null }),

    /** Как печатает эта касса: форма на узле, принтер на этой машине. */
    Printing(SettingsHousehold.Kkm, { it.groupPrinting }),

    /**
     * Чем торгует эта касса: вид отрасли её чеков.
     *
     * Своим разделом, а не вместе с настройками кассы: узел о нём
     * не знает — вид отрасли приходит ему с каждым чеком, — и режима
     * программирования настройка не требует. Заголовка нет: карточка
     * так и называется, и вторая строка тем же словом над ней ничего
     * не разделяла бы.
     */
    Trade(SettingsHousehold.Kkm, { null }),

    /** То, что хранит узел и принимает только в режиме программирования. */
    Service(SettingsHousehold.Kkm, { it.groupService }),

    Irreversible(SettingsHousehold.Kkm, { it.groupIrreversible })
}

/**
 * Имя настройки.
 *
 * Нужно, чтобы правило показа можно было проверить, не рисуя экран:
 * отбор возвращает имена, а не карточки. Порядок здесь — порядок
 * на экране.
 */
internal enum class Setting {
    Appearance, PanelBehaviour,
    NodeAddress, CabinetAddress, MapServices,
    NodeFacts, Updates, Debug,
    CurrentKkm, Programming, PrintForm, PrintTarget,
    Domain,
    Tax, OfdSync, OfdToken, Diagnostics, Decommission
}

/**
 * Настройка: своя карточка и условия показа.
 *
 * @param needsRegister настройке нужна выбранная касса.
 * @param adminOnly узел отвечает по ней только администратору.
 */
internal class SettingsCard(
    val setting: Setting,
    val group: SettingsGroup,
    val needsRegister: Boolean = false,
    val adminOnly: Boolean = false,
    val card: @Composable (Session) -> Unit
) {
    fun visible(hasRegister: Boolean, admin: Boolean): Boolean =
        (!needsRegister || hasRegister) && (!adminOnly || admin)
}

/** Что видно при таком месте и таких правах. */
internal fun visibleSettings(hasRegister: Boolean, admin: Boolean): List<Setting> =
    settingsCards.filter { it.visible(hasRegister, admin) }.map { it.setting }

/**
 * Весь список настроек приложения.
 *
 * Одно место, где видно, что у кассы вообще настраивается и что кому
 * доступно. Добавленная карточка ложится сюда со своим хозяйством
 * и условиями, а не в один из двух экранов, как было раньше.
 *
 * Порядок внутри хозяйства идёт от повседневного к необратимому: снятие
 * с учёта последним, чтобы его не нажимали по дороге к диагностике.
 */
internal val settingsCards = listOf(
    SettingsCard(Setting.Appearance, SettingsGroup.Look) { AppearanceCard(it) },
    SettingsCard(Setting.PanelBehaviour, SettingsGroup.Look) { PanelBehaviourCard(it) },

    // Адреса служб задают раньше, чем куда-либо входят: кассе без адреса
    // узла войти некуда. Кабинет стоит рядом с узлом, а не своей вкладкой:
    // его адрес тоже хранится на этой машине и ищется там же, где остальные.
    SettingsCard(Setting.NodeAddress, SettingsGroup.Addresses) { NodeAddressCard(it) },
    SettingsCard(Setting.CabinetAddress, SettingsGroup.Addresses) { CabinetAddressCard(it) },
    SettingsCard(Setting.MapServices, SettingsGroup.Addresses) { MapServicesCard(it) },

    SettingsCard(Setting.NodeFacts, SettingsGroup.Program) { NodeFactsCard(it) },
    // Версия кассы и выпуски не зависят ни от кассы, ни от прав: узнать,
    // что стоит и что вышло, можно с экрана входа.
    SettingsCard(Setting.Updates, SettingsGroup.Program) { UpdatesCard(it) },
    // Отладка нужна ровно тогда, когда войти нельзя: узел не отвечает,
    // список касс пуст. Условий у неё нет намеренно, а место — за тем,
    // что кассир читает каждый день.
    SettingsCard(Setting.Debug, SettingsGroup.Program) { DebugCard(it) },

    SettingsCard(Setting.CurrentKkm, SettingsGroup.Current, needsRegister = true) { CurrentKkmCard(it) },
    // Режим программирования стоит сразу под кассой, которой он
    // принадлежит: вход и выход в одном месте, и состояние видно оттуда,
    // откуда его меняют.
    SettingsCard(
        Setting.Programming,
        SettingsGroup.Current,
        needsRegister = true,
        adminOnly = true
    ) { ProgrammingCard(it) },

    // Печатная форма — настройка узла, и меняет её только администратор
    // в режиме программирования. Кассиру она показывалась карточкой,
    // в которой мертво всё: войти в режим он тоже не может.
    SettingsCard(
        Setting.PrintForm,
        SettingsGroup.Printing,
        needsRegister = true,
        adminOnly = true
    ) { PrintFormCard(it) },
    SettingsCard(Setting.PrintTarget, SettingsGroup.Printing, needsRegister = true) { PrintTargetCard(it) },

    // Отрасль стоит перед налогами: и то и другое решает, чем будет
    // наполнен каждый чек этой кассы, и владелец задаёт их в одном месте.
    // Кассиру выбор не показывается: отрасль задают при настройке кассы,
    // а не по ходу смены.
    SettingsCard(
        Setting.Domain,
        SettingsGroup.Trade,
        needsRegister = true,
        adminOnly = true
    ) { TradeDomainCard(it) },

    SettingsCard(Setting.Tax, SettingsGroup.Service, needsRegister = true, adminOnly = true) { TaxSettingsCard(it) },
    SettingsCard(Setting.OfdSync, SettingsGroup.Service, needsRegister = true, adminOnly = true) { OfdSyncCard(it) },
    SettingsCard(Setting.OfdToken, SettingsGroup.Service, needsRegister = true, adminOnly = true) { OfdTokenCard(it) },
    SettingsCard(Setting.Diagnostics, SettingsGroup.Service, needsRegister = true) { DiagnosticsCard(it) },

    SettingsCard(
        Setting.Decommission,
        SettingsGroup.Irreversible,
        needsRegister = true,
        adminOnly = true
    ) { DecommissionCard(it) }
)
