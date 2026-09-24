package kz.mybrain.superkassa.kassa

import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.workplace.model.WorkplaceLook
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory
import kz.mybrain.superkassa.presentation.common.message.Notices
import kz.mybrain.superkassa.presentation.common.model.Talk
import kz.mybrain.superkassa.presentation.common.model.WindowServices
import kz.mybrain.superkassa.strings.api.Language

/*
 * Общие службы окна поверх кассы проверок — для фабрик моделей любой
 * области: память рабочего места и вид окна — в памяти проверки, журнал
 * молчит, кассир говорит по-русски.
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
