package kz.mybrain.superkassa.presentation.setup

import kz.mybrain.superkassa.domain.kassa.port.Kassa
import kz.mybrain.superkassa.domain.log.port.Journal
import kz.mybrain.superkassa.domain.setup.port.SetupPorts
import kz.mybrain.superkassa.domain.setup.usecase.EnrollKkm
import kz.mybrain.superkassa.domain.setup.usecase.GetFactoryNumber
import kz.mybrain.superkassa.domain.setup.usecase.IssueKkmToken
import kz.mybrain.superkassa.domain.setup.usecase.ReadSetupContext
import kz.mybrain.superkassa.domain.setup.usecase.RememberSetup
import kz.mybrain.superkassa.domain.signin.model.SignIn
import kz.mybrain.superkassa.domain.signin.usecase.WorkOnKkm
import kz.mybrain.superkassa.domain.workplace.port.WorkplaceMemory

/**
 * Сценарии мастера подключения: касса процесса, пройденное и токен кабинета.
 *
 * Собираются из портов один раз на модель; модель знает только их.
 * Без кабинета токен выпускать некому ([issueToken] — `null`), и путь
 * у мастера один — вручную. Заведённая касса становится кассой рабочего
 * места ([workOn]) — вход открывается на ней.
 */
internal class SetupCases(kassa: Kassa, ports: SetupPorts, log: Journal, signIn: SignIn, memory: WorkplaceMemory) {
    val readContext = ReadSetupContext(kassa)
    val getFactoryNumber = GetFactoryNumber(kassa, ports.memory)
    val remember = RememberSetup(ports.memory)
    val enroll = EnrollKkm(kassa, log)
    val issueToken = ports.cabinet?.let(::IssueKkmToken)
    val workOn = WorkOnKkm(signIn, memory)

    /** Путь выбирают только там, где есть кабинет: без него путь один — вручную. */
    val choosing: Boolean = issueToken != null
}
