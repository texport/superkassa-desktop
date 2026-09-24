package kz.mybrain.superkassa.kassa

import kz.mybrain.superkassa.domain.journal.port.JournalPorts
import kz.mybrain.superkassa.domain.journal.port.NoDeliveries
import kz.mybrain.superkassa.domain.kassa.port.FixedDeliverySetup
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.kassa.port.KassaPorts
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.workplace.model.WorkplaceLook
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory
import kz.mybrain.superkassa.presentation.analytics.analyticsPorts
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.WindowServices
import kz.mybrain.superkassa.presentation.settings.MachinePorts
import kz.mybrain.superkassa.presentation.settings.settingsPorts
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.AreaPorts
import kz.mybrain.superkassa.strings.api.Language

/*
 * Зависимости экранов поверх кассы проверок.
 *
 * Значения кассы — в оснастке проверок (`CoreScene`), её видят проверки
 * всех модулей; контейнер окна знают только экраны, и собирается он здесь.
 */

/** Общие службы окна поверх [core]; вход и строка сообщений — свои. */
fun CoreScene.services(
    core: FakeCore,
    signIn: SignIn = SignIn(),
    notices: Notices = Notices(),
    memory: WorkplaceMemory = MemoryWorkplace()
) = services(core.kassa(), signIn, notices, memory)

/** Общие службы окна поверх кассы [kassa] — например, настоящего ядра на тестовом БФД. */
fun CoreScene.services(
    kassa: Kassa,
    signIn: SignIn = SignIn(),
    notices: Notices = Notices(),
    memory: WorkplaceMemory = MemoryWorkplace()
) = WindowServices(kassa, signIn, memory, WorkplaceLook(MemoryLook()), Talk(notices, SilentJournal) { Language.Ru })

/** Зависимости экранов поверх [core]; вход и строка сообщений — свои. */
fun CoreScene.app(
    core: FakeCore,
    signIn: SignIn = SignIn(),
    notices: Notices = Notices(),
    memory: WorkplaceMemory = MemoryWorkplace(),
    settings: MachinePorts = settingsPorts(),
    ports: KassaPorts = KassaPorts(FixedDeliverySetup())
) = app(core.kassa(), signIn, notices, memory, settings, ports = ports)

/** Зависимости экранов поверх кассы [kassa] — например, настоящего ядра на тестовом БФД. */
fun CoreScene.app(
    kassa: Kassa,
    signIn: SignIn = SignIn(),
    notices: Notices = Notices(),
    memory: WorkplaceMemory = MemoryWorkplace(),
    settings: MachinePorts = settingsPorts(),
    journal: JournalPorts = JournalPorts(NoDeliveries),
    ports: KassaPorts = KassaPorts(FixedDeliverySetup())
) = AppContainer(
    services = services(kassa, signIn, notices, memory),
    areas = AreaPorts(
        kassa = ports,
        journal = journal,
        settings = settings.settings,
        print = settings.print,
        update = settings.update,
        debug = settings.debug,
        analytics = analyticsPorts()
    )
)
