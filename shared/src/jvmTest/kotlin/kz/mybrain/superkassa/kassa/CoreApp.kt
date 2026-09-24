package kz.mybrain.superkassa.kassa

import kz.mybrain.superkassa.domain.journal.port.JournalPorts
import kz.mybrain.superkassa.domain.journal.port.NoDeliveries
import kz.mybrain.superkassa.domain.kassa.port.FixedDeliverySetup
import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.kassa.port.KassaPorts
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory
import kz.mybrain.superkassa.presentation.analytics.analyticsPorts
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.settings.MachinePorts
import kz.mybrain.superkassa.presentation.settings.settingsPorts
import kz.mybrain.superkassa.presentation.shell.AppContainer
import kz.mybrain.superkassa.presentation.shell.AreaPorts

/*
 * Зависимости экранов поверх кассы проверок.
 *
 * Значения кассы — в оснастке проверок (`CoreScene`), её видят проверки
 * всех модулей; контейнер окна знают только экраны, и собирается он здесь.
 */

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
) = AppContainer(services(kassa, signIn, notices, memory), areaPorts(settings, journal, ports))

/** Порты областей для проверок: машина в памяти, доставки нет, аналитика без сети. */
fun areaPorts(
    settings: MachinePorts = settingsPorts(),
    journal: JournalPorts = JournalPorts(NoDeliveries),
    kassa: KassaPorts = KassaPorts(FixedDeliverySetup())
) = AreaPorts(
    kassa = kassa,
    journal = journal,
    settings = settings.settings,
    print = settings.print,
    update = settings.update,
    debug = settings.debug,
    analytics = analyticsPorts()
)

/** Контейнер окна над кассой рабочего места [CoreDesk] — для снимков окна с каркасом. */
fun CoreDesk.app(): AppContainer = AppContainer(services, areaPorts(kassa = kassaPorts))
