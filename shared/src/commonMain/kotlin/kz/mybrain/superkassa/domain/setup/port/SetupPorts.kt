package kz.mybrain.superkassa.domain.setup.port

/**
 * Что мастеру подключения нужно снаружи: память пройденного и кабинет.
 *
 * Без кабинета мастер ведёт только ручной путь — идентификатор и токен
 * кассы, выданные в БФД без владельца: заводской номер даёт касса,
 * а заводит кассу касса процесса.
 *
 * @property cabinet кабинет БФД; `null` — на этой платформе кабинета нет.
 */
class SetupPorts(val memory: SetupMemory, val cabinet: SetupCabinet? = null)
